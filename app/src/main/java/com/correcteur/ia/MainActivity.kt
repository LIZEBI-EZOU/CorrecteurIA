package com.correcteur.ia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B8F4A)
private val GreenDark = Color(0xFF087F43)
private val Red = Color(0xFFF02B25)
private val Orange = Color(0xFFFF5A1F)
private val SoftGreen = Color(0xFFEAF8F0)
private val SoftRed = Color(0xFFFFECEA)

private val AppColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Red,
    onSecondary = Color.White,
    tertiary = Orange,
    background = Color(0xFFF7FAF8),
    surface = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContent { CorrecteurApp() }
    }
}

private fun openWeb(activity: ComponentActivity, url: String) {
    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrecteurApp(vm: CorrecteurViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    var vocabulary by remember { mutableStateOf("") }
    var protectedWord by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.refreshConnectivity() }

    MaterialTheme(colorScheme = AppColors) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(330.dp),
                    drawerContainerColor = Color.White
                ) {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.correcteur_logo),
                                contentDescription = null,
                                modifier = Modifier.size(58.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("CorrecteurIA", fontWeight = FontWeight.ExtraBold)
                                Text("Préférences & connexion", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        HorizontalDivider()

                        Text("☰ Paramètres", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = GreenDark)

                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftGreen)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("🌐 État Internet", fontWeight = FontWeight.Bold, color = GreenDark)
                                Text(
                                    if (state.onlineAvailable) "Connecté — la reformulation locale est désactivée."
                                    else "Hors connexion — la reformulation locale est autorisée.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(onClick = vm::refreshConnectivity) {
                                    Text("Actualiser")
                                }
                            }
                        }

                        Text("🤖 IA en ligne", fontWeight = FontWeight.Bold, color = Red)
                        Text(
                            "Pour utiliser l’IA en ligne depuis l’application, ajoutez votre clé API personnelle. Elle est conservée chiffrée sur l’appareil et n’est jamais incluse dans l’APK.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Clé API") },
                            placeholder = { Text("sk-…") },
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    vm.setOpenAiApiKey(apiKey)
                                    apiKey = ""
                                },
                                enabled = apiKey.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Green)
                            ) { Text("Enregistrer") }
                            OutlinedButton(onClick = vm::clearOpenAiApiKey) { Text("Retirer") }
                        }
                        if (state.openAiConnected) {
                            Text("✓ Accès IA en ligne configuré", color = GreenDark, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { openWeb(activity, "https://chatgpt.com/") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("💬 Ouvrir / connecter ChatGPT") }

                        OutlinedButton(
                            onClick = { openWeb(activity, "https://accounts.google.com/") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("G Se connecter avec Google") }

                        OutlinedButton(
                            onClick = { openWeb(activity, "https://platform.openai.com/api-keys") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🔑 Obtenir une clé API") }

                        Text(
                            "Important : une connexion à ChatGPT ou à Google dans le navigateur ne donne pas automatiquement à une application Android un accès API. L’API OpenAI utilise une clé API et sa facturation est séparée de l’abonnement ChatGPT.",
                            style = MaterialTheme.typography.labelSmall
                        )

                        HorizontalDivider()
                        Text("🪄 Styles de reformulation", fontWeight = FontWeight.Bold, color = GreenDark)
                        Text("Humanisé • Naturel • Professionnel • Formel • Simple • Chaleureux • Amical • Concis • Détaillé • Créatif")
                        Text("Le mode en ligne utilise votre IA configurée. Le mode local intervient uniquement sans connexion Internet.")

                        HorizontalDivider()
                        Text("🌍 Sites gratuits de reformulation", fontWeight = FontWeight.Bold, color = Red)
                        Text("Ces services s’ouvrent dans votre navigateur. L’application ne prétend pas les intégrer comme API.")
                        OutlinedButton(
                            onClick = { openWeb(activity, "https://languagetool.org/fr/reformuler-un-texte") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("LanguageTool — reformulation") }
                        OutlinedButton(
                            onClick = { openWeb(activity, "https://quillbot.com/fr/") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("QuillBot — reformulation") }

                        HorizontalDivider()
                        Text("🔒 Confidentialité", fontWeight = FontWeight.Bold, color = GreenDark)
                        Text("Le texte envoyé à l’IA en ligne quitte l’appareil et est traité par le service distant. En mode hors connexion, la reformulation reste sur l’appareil.")
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = AppColors.background,
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Text("☰", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.correcteur_logo),
                                    contentDescription = "Logo CorrecteurIA",
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("CorrecteurIA", fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        if (state.onlineAvailable) "● En ligne" else "● Hors ligne",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = GreenDark,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Green, GreenDark, Red)), RoundedCornerShape(24.dp))
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.correcteur_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(92.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("CORRECTEUR INTELLIGENT", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Écrivez mieux. À votre façon.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                    Text("Correction • reformulation • mémoire • IA en ligne", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.input,
                        onValueChange = vm::setInput,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp),
                        label = { Text("Votre texte") },
                        placeholder = { Text("Écrivez ou collez votre texte ici…") },
                        shape = RoundedCornerShape(18.dp)
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = vm::correct,
                            modifier = Modifier.weight(1f),
                            enabled = state.input.isNotBlank() && !state.busy,
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("✓ Corriger") }
                        Button(
                            onClick = vm::rewrite,
                            modifier = Modifier.weight(1f),
                            enabled = state.input.isNotBlank() && !state.busy,
                            colors = ButtonDefaults.buttonColors(containerColor = Red),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("✨ Reformuler") }
                    }

                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green)

                    if (state.output.isNotBlank()) {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SoftGreen)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Résultat", fontWeight = FontWeight.Bold, color = GreenDark)
                                Spacer(Modifier.height(6.dp))
                                Text(state.output, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = vm::useResult) { Text("Remplacer") }
                            OutlinedButton(onClick = vm::clearOutput) { Text("Effacer") }
                        }
                    }

                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = GreenDark)

                    HorizontalDivider()
                    Text("✨ Reformulation IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Red)
                    Text("Choisissez un style et une intensité. Si Internet est disponible, l’app utilise l’IA en ligne configurée. Sinon elle bascule automatiquement vers le moteur local.", style = MaterialTheme.typography.bodySmall)

                    Text("🎯 Style", fontWeight = FontWeight.Bold, color = GreenDark)
                    val styles = listOf("Standard", "Humanisé", "Naturel", "Professionnel", "Formel", "Simple", "Chaleureux", "Amical", "Concis", "Détaillé", "Créatif")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        styles.chunked(4).forEach { rowStyles ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowStyles.forEach { style ->
                                    FilterChip(
                                        selected = state.style == style,
                                        onClick = { vm.setStyle(style) },
                                        label = { Text(style) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreen, selectedLabelColor = GreenDark)
                                    )
                                }
                            }
                        }
                    }

                    Text("⚙️ Intensité", fontWeight = FontWeight.Bold, color = GreenDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Légère", "Équilibré", "Fort").forEach { intensity ->
                            FilterChip(
                                selected = state.rewriteIntensity == intensity,
                                onClick = { vm.setRewriteIntensity(intensity) },
                                label = { Text(intensity) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftRed, selectedLabelColor = Red)
                            )
                        }
                    }

                    HorizontalDivider()
                    Text("🧠 Mémoire personnelle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                    Text("Les préférences restent locales.", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(value = vocabulary, onValueChange = { vocabulary = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ajouter au vocabulaire") }, singleLine = true)
                    Button(
                        onClick = { if (vocabulary.isNotBlank()) { vm.addVocabulary(vocabulary); vocabulary = "" } },
                        enabled = vocabulary.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text("➕ Mémoriser") }

                    OutlinedTextField(value = protectedWord, onValueChange = { protectedWord = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Mot à protéger") }, singleLine = true)
                    Button(
                        onClick = { if (protectedWord.isNotBlank()) { vm.addProtectedWord(protectedWord); protectedWord = "" } },
                        enabled = protectedWord.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) { Text("🛡️ Protéger") }

                    Text("🔁 Préférence de correction", fontWeight = FontWeight.Bold, color = GreenDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = from, onValueChange = { from = it }, modifier = Modifier.weight(1f), label = { Text("Avant") }, singleLine = true)
                        OutlinedTextField(value = to, onValueChange = { to = it }, modifier = Modifier.weight(1f), label = { Text("Après") }, singleLine = true)
                    }
                    Button(
                        onClick = { if (from.isNotBlank() && to.isNotBlank()) { vm.learnReplacement(from, to); from = ""; to = "" } },
                        enabled = from.isNotBlank() && to.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text("🧠 Apprendre") }

                    HorizontalDivider()
                    Text("🗂️ Historique", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                    state.history.take(5).forEach { item ->
                        OutlinedButton(onClick = { vm.loadHistory(item) }, modifier = Modifier.fillMaxWidth()) {
                            Text(item.mode + " — " + item.input.take(70))
                        }
                    }
                    Row {
                        TextButton(onClick = vm::clearHistory) { Text("Effacer l’historique", color = Red) }
                        TextButton(onClick = vm::clearPersonalMemory) { Text("Effacer préférences") }
                        TextButton(onClick = vm::clearMemory) { Text("Tout effacer", color = Red) }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}
