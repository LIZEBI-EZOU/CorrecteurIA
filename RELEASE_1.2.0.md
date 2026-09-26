# CorrecteurIA 1.2.0 — état final

Date de finalisation : 2026-09-26

## Build Android
- Version : 1.2.0
- VersionCode : 3
- Build CI : réussi
- Tests unitaires : réussis
- APK CI : généré avec succès
- SHA-256 de l'APK final : `dd88ea3aeefe89d4bf98a189d23e6bcdedf8a13d654f18b6d45dd55cad2ee03c`

## Backend Render
- Service : `correcteuria-api`
- URL : https://correcteuria-api.onrender.com
- État : LIVE
- `/health` : HTTP 200
- `/ready` : HTTP 200
- Correction en ligne `/v1/correct` : test HTTP 200
- Clé OpenAI : configurée côté Render uniquement, jamais intégrée à l'APK

## Architecture
- L'APK appelle le backend pour les fonctions en ligne.
- La clé OpenAI reste côté serveur.
- Les fonctions locales/hors-ligne restent disponibles.
- Les fichiers de signature et secrets restent exclus du dépôt.

## Surveillance
- Workflow GitHub de surveillance du serveur : `.github/workflows/server-monitor.yml`
- CI Android : `.github/workflows/android.yml`
- CI serveur : `.github/workflows/server.yml`

## Note
La reformulation OpenAI peut retourner HTTP 429 si le compte API associé n'a plus de crédit disponible. Cela concerne la capacité API du compte, pas la compilation ou la signature de l'APK.
