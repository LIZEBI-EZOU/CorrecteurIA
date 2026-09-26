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
