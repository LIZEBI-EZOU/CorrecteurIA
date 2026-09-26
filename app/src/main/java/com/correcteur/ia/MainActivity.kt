package com.correcteur.ia
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{CorrecteurApp()}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CorrecteurApp(vm:CorrecteurViewModel=viewModel()){
 val s by vm.uiState.collectAsState()
 MaterialTheme{Scaffold(topBar={TopAppBar(title={Column{Text("Correcteur IA",fontWeight=FontWeight.Bold);Text(if(s.internetEnabled)"Mode hybride • Internet + hors connexion" else "Mode privé • hors connexion",style=MaterialTheme.typography.labelSmall)}})}){p->
  Column(Modifier.padding(p).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
   OutlinedTextField(s.input,vm::setInput,Modifier.fillMaxWidth().heightIn(min=220.dp),label={Text("Votre texte")},placeholder={Text("Écrivez ou collez votre texte ici…")})
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(vm::correct,enabled=s.input.isNotBlank()&&!s.busy){Text("✓ Corriger")};OutlinedButton(vm::rewrite,enabled=s.input.isNotBlank()&&!s.busy){Text("✨ Reformuler")}}
   if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
   if(s.output.isNotBlank()){Text("Résultat",style=MaterialTheme.typography.titleMedium);Card(Modifier.fillMaxWidth()){Text(s.output,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyLarge)};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(vm::useResult){Text("Remplacer")};OutlinedButton(vm::clearOutput){Text("Effacer")}}}
   Text(s.message,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
   HorizontalDivider();Text("⚡ Options intelligentes",style=MaterialTheme.typography.titleMedium)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("Correction Internet",fontWeight=FontWeight.SemiBold);Text(if(s.internetEnabled)"Internet si disponible, puis secours local." else "Aucun texte n’est envoyé en ligne.")};Switch(s.internetEnabled,vm::setInternetEnabled)}
   HorizontalDivider();Text("🧠 Mémoire locale",style=MaterialTheme.typography.titleMedium);Text("Les 20 dernières corrections restent sur cet appareil.")
   s.history.take(5).forEach{item->OutlinedButton({vm.loadHistory(item)},Modifier.fillMaxWidth()){Text(item.mode+" — "+item.input.take(70))}}
   if(s.history.isNotEmpty())TextButton(vm::clearMemory){Text("Effacer toute la mémoire")}
   HorizontalDivider();Text("🔒 Confidentialité",style=MaterialTheme.typography.titleMedium);Text("En mode Internet, le texte peut être envoyé au service LanguageTool en ligne. Désactivez l’option pour rester entièrement hors connexion.")
  }
 }}
}