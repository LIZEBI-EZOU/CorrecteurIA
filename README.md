# Correcteur IA — Android

Application Android de correction et reformulation en français.

## Fonctionnalités
- Correction locale avec LanguageTool.
- Fallback offline conservateur.
- Interface Jetpack Compose.
- IA on-device via ML Kit GenAI lorsque Gemini Nano/AICore est disponible.
- Aucun envoi cloud automatique.

## Construction
- JDK 17
- Android SDK 37
- Android Gradle Plugin 9.2.0
- Gradle 9.4.1

La CI GitHub Actions construit automatiquement un APK debug et publie l'APK comme artifact.

Pour une release signée, utiliser un keystore local ou un secret GitHub. Ne jamais versionner un keystore ni ses mots de passe.

## Serveur intermédiaire CorrecteurIA

Le projet inclut maintenant un serveur Node/Express dans `server/`. L’APK ne contient plus de clé OpenAI : le serveur conserve `OPENAI_API_KEY` dans son environnement et appelle l’API OpenAI pour la reformulation. Le modèle par défaut est `gpt-5.6-luna`, choisi pour les usages à volume élevé et sensibles au coût. 

Le serveur expose :
- `GET /health` pour la santé de l’instance ;
- `GET /ready` pour vérifier que la clé IA est configurée ;
- `POST /v1/rewrite` pour la reformulation ;
- `POST /v1/correct` pour la correction en ligne ;
- `GET /metrics` protégé par un jeton d’administration.

Le serveur applique des limites de débit, une taille maximale de texte, des délais réseau, des identifiants de requête et des journaux sans le contenu des textes. Le blueprint Render active aussi le health check sur `/health`. Render utilise ce type de health check pour vérifier qu’un service est prêt et peut redémarrer une instance qui échoue durablement.

### Mise en production

1. Déployer le dossier `server/` sur Render avec `server/render.yaml`.
2. Dans les variables d’environnement Render, renseigner **OPENAI_API_KEY**. Render recommande de conserver les clés et secrets dans les variables d’environnement plutôt que dans le dépôt.
3. Garder `OPENAI_MODEL=gpt-5.6-luna` ou choisir un autre modèle compatible.
4. L’URL attendue par l’APK est par défaut : `https://correcteuria-api.onrender.com`.

> Le serveur gratuit Render peut se mettre en veille après une période d’inactivité ; le premier appel peut donc être plus lent. Pour une disponibilité permanente, utiliser une instance payante.

Important : une clé OpenAI réelle ne doit pas être écrite dans l’APK, le dépôt GitHub ou le code source. Elle doit rester côté serveur.