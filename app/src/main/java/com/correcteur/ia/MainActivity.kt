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
 var vocab by remember{mutableStateOf("")}
 var protected by remember{mutableStateOf("")}
 var from by remember{mutableStateOf("")}
 var to by remember{mutableStateOf("")}
 MaterialTheme{Scaffold(topBar={TopAppBar(title={Column{Text("Correcteur IA",fontWeight=FontWeight.Bold);Text(if(s.internetEnabled)"Mode hybride • Internet + hors connexion" else "Mode privé • hors connexion",style=MaterialTheme.typography.labelSmall)}})}){p->
  Column(Modifier.padding(p).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
   OutlinedTextField(s.input,vm::setInput,Modifier.fillMaxWidth().heightIn(min=220.dp),label={Text("Votre texte")},placeholder={Text("Écrivez ou collez votre texte ici…")})
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(vm::correct,enabled=s.input.isNotBlank()&&!s.busy){Text("✓ Corriger")};OutlinedButton(vm::rewrite,enabled=s.input.isNotBlank()&&!s.busy){Text("✨ Reformuler")}}
   if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
   if(s.output.isNotBlank()){Text("Résultat",style=MaterialTheme.typography.titleMedium);Card(Modifier.fillMaxWidth()){Text(s.output,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyLarge)};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(vm::useResult){Text("Remplacer")};OutlinedButton(vm::clearOutput){Text("Effacer")}}}
   Text(s.message,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
   HorizontalDivider()
   Text("⚡ Options intelligentes",style=MaterialTheme.typography.titleMedium)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("Correction Internet",fontWeight=FontWeight.SemiBold);Text(if(s.internetEnabled)"Internet si disponible, puis secours local." else "Aucun texte n’est envoyé en ligne.")};Switch(s.internetEnabled,vm::setInternetEnabled)}
   Text("🎯 Style préféré",fontWeight=FontWeight.SemiBold)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Standard","Professionnel","Simple","Chaleureux").forEach{style->FilterChip(selected=s.style==style,onClick={vm.setStyle(style)},label={Text(style)})}}
   HorizontalDivider()
   Text("🧠 Mémoire personnelle avancée",style=MaterialTheme.typography.titleMedium)
   Text("La mémoire reste sur cet appareil : elle apprend votre vocabulaire, vos mots protégés et vos corrections préférées.")
   OutlinedTextField(vocab,{vocab=it},Modifier.fillMaxWidth(),label={Text("Ajouter au vocabulaire")},singleLine=true)
   Button(onClick={if(vocab.isBlank()){}else{vm.addVocabulary(vocab);vocab=""},enabled=vocab.isNotBlank()}{Text("➕ Mémoriser ce mot")})
   if(s.vocabulary.isNotEmpty()){Text("Vocabulaire : "+s.vocabulary.joinToString(" • "));Row{TextButton(onClick={vm.removeVocabulary(s.vocabulary.last())}){Text("Retirer le dernier")}}}
   OutlinedTextField(protected,{protected=it},Modifier.fillMaxWidth(),label={Text("Mot à ne jamais corriger")},singleLine=true)
   Button(onClick={if(protected.isBlank()){}else{vm.addProtectedWord(protected);protected=""},enabled=protected.isNotBlank()}{Text("🛡️ Protéger ce mot")})
   if(s.protectedWords.isNotEmpty()){Text("Protégés : "+s.protectedWords.joinToString(" • "));Row{TextButton(onClick={vm.removeProtectedWord(s.protectedWords.last())}){Text("Retirer le dernier")}}}
   Text("🔁 Correction préférée",fontWeight=FontWeight.SemiBold)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(from,{from=it},Modifier.weight(1f),label={Text("Avant")},singleLine=true);OutlinedTextField(to,{to=it},Modifier.weight(1f),label={Text("Après")},singleLine=true)}
   Button(onClick={if(from.isBlank()||to.isBlank()){}else{vm.learnReplacement(from,to);from="";to=""},enabled=from.isNotBlank()&&to.isNotBlank()}{Text("🧠 Apprendre cette correction")})
   if(s.learnedReplacements.isNotEmpty()){Text("Préférences mémorisées :");s.learnedReplacements.entries.take(8).forEach{(a,b)->Text("• $a → $b")};Row{TextButton(onClick={vm.removeReplacement(s.learnedReplacements.keys.last()}){Text("Oublier la dernière")}}}
   HorizontalDivider()
   Text("🗂️ Historique",style=MaterialTheme.typography.titleMedium)
   Text("Les 20 dernières corrections/reformulations restent sur l’appareil.")
   s.history.take(5).forEach{item->OutlinedButton({vm.loadHistory(item)},Modifier.fillMaxWidth()){Text(item.mode+" — "+item.input.take(70))}}
   Row{if(s.history.isNotEmpty())TextButton(vm::clearHistory){Text("Effacer l’historique")};TextButton(vm::clearPersonalMemory){Text("Effacer préférences")};TextButton(vm::clearMemory){Text("Tout effacer")}}
   HorizontalDivider()
   Text("🔒 Confidentialité",style=MaterialTheme.typography.titleMedium)
   Text("La mémoire avancée est locale. En mode Internet, le texte peut être envoyé à LanguageTool en ligne. Désactivez Internet pour rester hors connexion.")
  }
 }}
}
