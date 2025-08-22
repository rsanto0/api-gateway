# Script Completo de Setup para GitHub - PowerShell 7
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   SCRIPT COMPLETO PARA GITHUB" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ==============================
# FUNÇÃO: Configurar SSH
# ==============================
function Setup-GitHubSSH {
    param([string]$Email)
    
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "    CONFIGURANDO CHAVE SSH" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host ""
    
    if (-not $Email) {
        $Email = Read-Host "Digite seu email do GitHub"
    }

    $sshKeyPath = "$env:USERPROFILE\.ssh\id_ed25519"

    try {
        # Criar pasta .ssh se não existir
        if (-not (Test-Path -Path "$env:USERPROFILE\.ssh")) {
            New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh" | Out-Null
            Write-Host "[OK] Pasta .ssh criada" -ForegroundColor Green
        }

        # Gerar chave SSH ed25519
        if (-not (Test-Path "$sshKeyPath")) {
            ssh-keygen -t ed25519 -C $Email -f $sshKeyPath -N '""'
            Write-Host "[OK] Chave SSH gerada em: $sshKeyPath" -ForegroundColor Green
        } else {
            Write-Host "[AVISO] Já existe uma chave SSH em $sshKeyPath" -ForegroundColor Yellow
        }

        # Iniciar o ssh-agent
        try {
            if ((Get-Service ssh-agent).Status -ne 'Running') {
                Start-Process powershell -Verb RunAs -ArgumentList "Set-Service ssh-agent -StartupType Automatic; Start-Service ssh-agent" -Wait -WindowStyle Hidden
            }
            ssh-add $sshKeyPath 2>$null
            Write-Host "[OK] ssh-agent configurado" -ForegroundColor Green
        } catch {
            Write-Host "[AVISO] Erro no ssh-agent. Execute como administrador ou configure manualmente" -ForegroundColor Yellow
            Write-Host "Comando manual: ssh-add $sshKeyPath" -ForegroundColor Yellow
        }

        # Copiar chave pública para área de transferência
        $publicKey = Get-Content "$sshKeyPath.pub" -Raw
        Set-Clipboard $publicKey.Trim()
        Write-Host "[OK] Sua chave pública foi copiada para a área de transferência." -ForegroundColor Green

        # Mostrar chave pública
        Write-Host "`nSua chave pública:" -ForegroundColor Blue
        Write-Host $publicKey -ForegroundColor Green

        # Abrir página do GitHub para adicionar chave
        Write-Host "`nAbrindo página do GitHub para adicionar a chave..." -ForegroundColor Blue
        Start-Process "https://github.com/settings/ssh/new"
        Write-Host "Cole a chave no GitHub e pressione Enter para testar a conexão..." -ForegroundColor Yellow
        Read-Host

        # Teste de conexão com GitHub
        Write-Host "Testando conexão com GitHub..." -ForegroundColor Blue
        Write-Host "Se aparecer pergunta sobre fingerprint, digite 'yes' e pressione Enter" -ForegroundColor Yellow
        $sshTest = ssh -T git@github.com 2>&1
        if ($sshTest -match "successfully authenticated") {
            Write-Host "✅ Conexão SSH configurada com sucesso!" -ForegroundColor Green
            return $true
        } else {
            Write-Host "⚠️  Teste de SSH:" -ForegroundColor Yellow
            Write-Host $sshTest
            $retry = Read-Host "Deseja tentar novamente? (s/n)"
            if ($retry -eq 's' -or $retry -eq 'S') {
                return Setup-GitHubSSH -Email $Email
            }
            return $false
        }

    } catch {
        Write-Host "[ERRO] $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# ==============================
# INÍCIO DO SCRIPT PRINCIPAL
# ==============================

# 1. Verificar se existe chave SSH
$sshKeyPath = "$env:USERPROFILE\.ssh\id_ed25519"
if (-not (Test-Path $sshKeyPath)) {
    Write-Host "❌ Chave SSH não encontrada!" -ForegroundColor Red
    Write-Host ""
    $setupSSH = Read-Host "Deseja configurar chave SSH agora? (s/n)"
    
    if ($setupSSH -eq 's' -or $setupSSH -eq 'S') {
        $sshSuccess = Setup-GitHubSSH
        if (-not $sshSuccess) {
            Write-Host "❌ Falha na configuração SSH. Saindo..." -ForegroundColor Red
            Read-Host "Pressione Enter para sair"
            exit
        }
    } else {
        Write-Host "❌ Chave SSH é necessária. Saindo..." -ForegroundColor Red
        Read-Host "Pressione Enter para sair"
        exit
    }
} else {
    Write-Host "✅ Chave SSH encontrada" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    CONFIGURANDO REPOSITÓRIO GIT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 2. Verificar se já existe repositório Git
if (Test-Path ".git") {
    Write-Host "Repositório Git já existe nesta pasta." -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "Inicializando repositório Git..." -ForegroundColor Green
    git init
    Write-Host ""
}

# 3. Verificar configuração do Git
$gitName = git config --global user.name 2>$null
$gitEmail = git config --global user.email 2>$null

if (-not $gitName) {
    $gitName = Read-Host "Digite seu nome para o Git"
    git config --global user.name $gitName
}

if (-not $gitEmail) {
    $gitEmail = Read-Host "Digite seu email para o Git"
    git config --global user.email $gitEmail
}

Write-Host "Configuração atual:" -ForegroundColor Blue
Write-Host "Nome: $gitName"
Write-Host "Email: $gitEmail"
Write-Host ""

# 4. Adicionar arquivos
Write-Host "Adicionando arquivos..." -ForegroundColor Green
git add .
Write-Host ""

# 5. Commit
$commitMsg = Read-Host "Digite a mensagem do commit (ou Enter para Initial commit)"
if (-not $commitMsg) { $commitMsg = "Initial commit" }
git commit -m $commitMsg
Write-Host ""

# 6. Informações do repositório GitHub
$githubUser = Read-Host "Digite seu usuário do GitHub"

# Sugerir nome da pasta atual como padrão
$currentFolder = Split-Path -Leaf (Get-Location)
$repoNamePrompt = "Digite o nome do repositório no GitHub (ou Enter para '$currentFolder')"
$repoName = Read-Host $repoNamePrompt
if (-not $repoName) { $repoName = $currentFolder }
Write-Host ""

# 7. Perguntar se repositório já existe
$repoExists = Read-Host "O repositório '$repoName' já existe no GitHub? (s/n)"

if ($repoExists -ne 's' -and $repoExists -ne 'S') {
    # Repositório não existe - criar via API
    Write-Host ""
    Write-Host "📝 Criando repositório no GitHub..." -ForegroundColor Yellow
    $githubToken = Read-Host "Digite seu Personal Access Token do GitHub" -AsSecureString
    $token = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($githubToken))
    
    # Criar repositório via API
    $body = @{
        name = $repoName
        private = $false
    } | ConvertTo-Json

    $headers = @{
        "Authorization" = "token $token"
        "Content-Type" = "application/json"
    }

    try {
        $response = Invoke-RestMethod -Uri "https://api.github.com/user/repos" -Method Post -Body $body -Headers $headers
        Write-Host "✅ Repositório criado com sucesso!" -ForegroundColor Green
    } catch {
        Write-Host "❌ Erro ao criar repositório:" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        Write-Host ""
        Write-Host "Criando manualmente..." -ForegroundColor Yellow
        Start-Process "https://github.com/new"
        Read-Host "Crie o repositório manualmente e pressione Enter para continuar"
    }
    Write-Host ""
}

# 8. Adicionar remote SSH
Write-Host "Configurando remote SSH..." -ForegroundColor Green
git remote remove origin 2>$null
git remote add origin "git@github.com:$githubUser/$repoName.git"

# 9. Testar conexão SSH
Write-Host "Testando conexão SSH com GitHub..." -ForegroundColor Green
$sshTest = ssh -T git@github.com 2>&1
if ($sshTest -match "successfully authenticated") {
    Write-Host "✅ Conexão SSH funcionando!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Teste de SSH:" -ForegroundColor Yellow
    Write-Host $sshTest
}
Write-Host ""

# 10. Push
Write-Host "Enviando código para o GitHub via SSH..." -ForegroundColor Green
git branch -M main
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "   CÓDIGO ENVIADO COM SUCESSO!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Repositório: https://github.com/$githubUser/$repoName" -ForegroundColor Blue
} else {
    Write-Host ""
    Write-Host "❌ Erro no push. Verifique se:" -ForegroundColor Red
    Write-Host "1. O repositório existe no GitHub" -ForegroundColor Yellow
    Write-Host "2. Sua chave SSH está configurada corretamente" -ForegroundColor Yellow
    Write-Host "3. Você tem permissão no repositório" -ForegroundColor Yellow
}

Read-Host "Pressione Enter para sair"