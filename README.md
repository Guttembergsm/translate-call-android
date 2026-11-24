# Translate Call — Prova de conceito Android

Projeto inicial para o realme 12 5G / Android 16.

## O que esta versão faz
- Solicita permissão de microfone.
- Mede o nível de áudio captado pelo microfone.
- Abre o reconhecimento de fala do Android configurado para inglês (en-US).
- Mostra orientações para testar uma ligação com viva-voz.

## Limitação importante
Este APK **não captura diretamente o áudio interno da chamada telefônica e ainda não traduz conversas**. Android restringe o acesso ao áudio de chamadas para aplicativos comuns. O teste com viva-voz verifica apenas o som que chega ao microfone do aparelho. O resultado não garante que a voz remota esteja disponível ao app.

## Compilação
Requer Android SDK 36 e JDK 17. No GitHub Actions, o workflow `.github/workflows/build-apk.yml` compila o APK de depuração e publica o artefato para download.

## Instalação
Baixe `app-debug.apk` do artefato do GitHub Actions e instale no Android. Conceda a permissão de microfone quando solicitada.

## Próximas etapas
1. Validar captura acústica durante chamada com viva-voz.
2. Avaliar reconhecimento de voz contínuo.
3. Integrar tradução inglês-português e síntese de voz.
4. Investigar alternativas suportadas para áudio bidirecional em chamadas convencionais.
