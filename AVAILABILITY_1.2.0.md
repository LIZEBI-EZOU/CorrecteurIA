# CorrecteurIA — Disponibilité 1.2.0

## Backend
- Service Render : correcteuria-api
- URL : https://correcteuria-api.onrender.com
- Déploiement : dep-das2q7bncjis73fkuir0
- Commit serveur : ef89951f9761b06376984c090458b2fc99d073d8
- Statut au 26 septembre 2026 : LIVE
- Endpoint racine : /
- Santé : /health
- Disponibilité : /ready
- Correction : /v1/correct
- Reformulation : /v1/rewrite

## Fiabilisation Android
- Vérification du serveur avec délais élargis.
- Jusqu'à 3 tentatives automatiques avec temporisation progressive.
- Requêtes HTTP fermées proprement.
- Correction en ligne avec 3 tentatives.
- Reformulation en ligne avec 3 tentatives.
- Moteur local de secours si le serveur est temporairement inaccessible.
- Le secret OPENAI_API_KEY reste exclusivement côté serveur.

## Surveillance
Le workflow GitHub Actions `.github/workflows/server-monitor.yml` vérifie `/health` et `/ready` toutes les 5 minutes avec plusieurs tentatives.

## Limitation externe
Les journaux Render ont également montré que l'API OpenAI peut répondre `429 credit_balance_exhausted`. La connectivité du serveur et la disponibilité du compte OpenAI sont deux sujets distincts : un crédit API disponible est nécessaire pour la reformulation en ligne.
