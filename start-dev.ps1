$ErrorActionPreference = "Stop"

function Resolve-MySqlExecutable {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ExecutableName
    )

    $command = Get-Command $ExecutableName -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) {
        return $command.Source
    }

    $searchRoots = @()
    if ($env:MYSQL_BASE_DIR -and (Test-Path $env:MYSQL_BASE_DIR)) {
        $searchRoots += $env:MYSQL_BASE_DIR
    }
    if (Test-Path "C:\Program Files\MySQL") {
        $searchRoots += Get-ChildItem "C:\Program Files\MySQL" -Directory |
            Sort-Object Name -Descending |
            Select-Object -ExpandProperty FullName
    }

    foreach ($root in $searchRoots | Select-Object -Unique) {
        $directCandidate = Join-Path $root "bin\$ExecutableName"
        if (Test-Path $directCandidate) {
            return $directCandidate
        }

        $recursiveCandidate = Get-ChildItem -Path $root -Recurse -File -Filter $ExecutableName -ErrorAction SilentlyContinue |
            Sort-Object FullName |
            Select-Object -First 1 -ExpandProperty FullName
        if ($recursiveCandidate) {
            return $recursiveCandidate
        }
    }

    throw "No se encontro $ExecutableName. Instala MySQL Server o define MYSQL_BASE_DIR."
}

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        return (Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded
    } catch {
        return $false
    }
}

function Test-MySqlReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MySqlExe,
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        & $MySqlExe --protocol=TCP -h 127.0.0.1 -P $Port -u root -e "SELECT 1" 2>$null | Out-Null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Get-LocalMySqlPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MySqlExe
    )

    $candidatePorts = @()
    if ($env:LOCAL_MYSQL_PORT) {
        $candidatePorts += [int]$env:LOCAL_MYSQL_PORT
    } else {
        $candidatePorts += 3306, 3307, 3308, 3309
    }

    foreach ($port in $candidatePorts) {
        if (-not (Test-TcpPort -Port $port)) {
            return $port
        }

        if (Test-MySqlReady -MySqlExe $MySqlExe -Port $port) {
            return $port
        }
    }

    throw "No se encontro un puerto disponible para MySQL local. Prueba definiendo LOCAL_MYSQL_PORT."
}

function Get-MySqlVersionTag {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MysqldExe
    )

    try {
        $versionOutput = & $MysqldExe --version 2>$null
        if ($versionOutput -match "Ver\s+([0-9]+\.[0-9]+\.[0-9]+)") {
            return $Matches[1].Replace(".", "-")
        }
    } catch {
    }

    return ((Get-Item $MysqldExe).VersionInfo.FileVersion -replace "[^0-9.]", "").Replace(".", "-")
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mysqldExe = Resolve-MySqlExecutable -ExecutableName "mysqld.exe"
$mysqlClientExe = Resolve-MySqlExecutable -ExecutableName "mysql.exe"
$mysqlBaseDir = Split-Path -Parent (Split-Path -Parent $mysqldExe)
$mysqlVersionTag = Get-MySqlVersionTag -MysqldExe $mysqldExe
$localMySqlPort = Get-LocalMySqlPort -MySqlExe $mysqlClientExe
$mysqlRoot = Join-Path $projectRoot "target\mysql-local-$mysqlVersionTag"
$mysqlDataDir = Join-Path $mysqlRoot "data"
$mysqlLogsDir = Join-Path $mysqlRoot "logs"
$mysqlInitLog = Join-Path $mysqlLogsDir "mysql-init.err"
$mysqlRunLog = Join-Path $mysqlLogsDir "mysql-run.log"
$mysqlRunErr = Join-Path $mysqlLogsDir "mysql-run.err"
$backendLog = Join-Path $projectRoot "target\frontend-connect.log"
$datasourceUrl = "jdbc:mysql://127.0.0.1:$localMySqlPort/gestion_salas?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Bogota"

New-Item -ItemType Directory -Force -Path $mysqlDataDir | Out-Null
New-Item -ItemType Directory -Force -Path $mysqlLogsDir | Out-Null

if (-not (Test-Path (Join-Path $mysqlDataDir "auto.cnf"))) {
    & $mysqldExe `
        --initialize-insecure `
        --basedir=$($mysqlBaseDir.Replace("\", "/")) `
        --datadir=$($mysqlDataDir.Replace("\", "/")) `
        --log-error=$($mysqlInitLog.Replace("\", "/"))
}

$mysqlReady = Test-MySqlReady -MySqlExe $mysqlClientExe -Port $localMySqlPort

if (-not $mysqlReady) {
    Start-Process -FilePath $mysqldExe `
        -ArgumentList @(
            "--basedir=`"$($mysqlBaseDir.Replace('\', '/'))`"",
            "--datadir=`"$($mysqlDataDir.Replace('\', '/'))`"",
            "--port=$localMySqlPort",
            "--bind-address=127.0.0.1",
            "--mysqlx=0",
            "--log-error=`"$($mysqlRunErr.Replace('\', '/'))`""
        ) `
        -WindowStyle Hidden

    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        if (Test-MySqlReady -MySqlExe $mysqlClientExe -Port $localMySqlPort) {
            $mysqlReady = $true
            break
        }
    }
}


if (-not $mysqlReady) {
    throw "MySQL local no quedo disponible en el puerto $localMySqlPort."
}

$backendReady = $false
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -Method Get -UseBasicParsing
    $backendReady = $response.StatusCode -eq 200
} catch {
    $backendReady = $false
}

if (-not $backendReady) {
    Start-Process -FilePath "powershell.exe" `
        -ArgumentList "-NoProfile", "-Command", "& { Set-Location `'$projectRoot`'; `$env:SPRING_DATASOURCE_URL = `'$datasourceUrl`'; `$env:SPRING_DATASOURCE_USERNAME = `'root`'; `$env:SPRING_DATASOURCE_PASSWORD = `'`'; .\mvnw.cmd spring-boot:run *> `'$backendLog`' }" `
        -WindowStyle Hidden

    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -Method Get -UseBasicParsing
            if ($response.StatusCode -eq 200) {
                $backendReady = $true
                break
            }
        } catch {
        }
    }
}

if (-not $backendReady) {
    throw "El backend no quedo disponible en http://localhost:8080."
}

$seedSql = @"
CREATE DATABASE IF NOT EXISTS gestion_salas;
USE gestion_salas;
INSERT INTO usuario (nombre, correo, password, rol, id_facultad)
SELECT 'Secretaria Ingenieria', 'secretaria.ingenieria@uao.edu.co', 'ClaveSegura1!', 'SECRETARIA', 1
WHERE NOT EXISTS (
    SELECT 1 FROM usuario WHERE correo = 'secretaria.ingenieria@uao.edu.co'
);
INSERT INTO usuario (nombre, correo, password, rol, id_facultad)
SELECT 'Docente Ingenieria', 'docente.ingenieria@uao.edu.co', 'ClaveSegura1!', 'DOCENTE', 1
WHERE NOT EXISTS (
    SELECT 1 FROM usuario WHERE correo = 'docente.ingenieria@uao.edu.co'
);
"@

& $mysqlClientExe --protocol=TCP -h 127.0.0.1 -P $localMySqlPort -u root -e $seedSql | Out-Null

Write-Output "Backend listo en http://localhost:8080"
