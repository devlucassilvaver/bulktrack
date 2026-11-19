# Execute em Windows PowerShell ou PowerShell 7.
$env:DB_URL = 'jdbc:mysql://localhost:3306/bulktrack'
$env:DB_USERNAME = 'bulktrack_local'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha MySQL' -AsSecureString)).Password

# Descomente para provisionar uma conta local.
# $env:BULKTRACK_USUARIO = Read-Host 'Usuário local'
# $env:BULKTRACK_SENHA = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha local' -AsSecureString)).Password
# $env:BULKTRACK_PERFIL_LEGADO = '123'
