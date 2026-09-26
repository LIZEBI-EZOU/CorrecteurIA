package com.correcteur.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Blue=Color(0xFF1261D6)
private val DeepBlue=Color(0xFF073B8F)
private val Red=Color(0xFFE32636)
private val SoftBlue=Color(0xFFEAF2FF)
private val SoftRed=Color(0xFFFFECEE)

private val AppColors=lightColorScheme(primary=Blue,onPrimary=Color.White,secondary=Red,onSecondary=Color.White,tertiary=DeepBlue,background=Color(0xFFF7F9FC),surface=Color.White)

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{CorrecteurApp()}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CorrecteurApp(vm:CorrecteurViewModel=viewModel()){
 val s by vm.uiState.collectAsState()
 var vocab by remember{mutableStateOf("")};var protected by remember{mutableStateOf("")};var from by remember{mutableStateOf("")};var to by remember{mutableStateOf("")}
 MaterialTheme(colorScheme = AppColors) {
  Scaffold(
    containerColor = AppColors.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("CorrecteurIA", fontWeight = FontWeight.ExtraBold)
            Text(
              if (s.internetEnabled) "⚡ Internet + hors connexion" else "🔒 Mode privé",
              style = MaterialTheme.typography.labelSmall
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DeepBlue,
          titleContentColor = Color.White,
          navigationIconContentColor = Color.White
        )
      )
    }
  ) { p ->
  Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Blue,DeepBlue,Red)),RoundedCornerShape(22.dp)).padding(20.dp)){Column{Text("CORRECTEUR INTELLIGENT",color=Color.White,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold);Text("Écrivez mieux. Plus vite. À votre façon.",color=Color.White,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold);Spacer(Modifier.height(6.dp));Text("IA locale • mémoire personnalisée • Internet optionnel",color=Color.White.copy(alpha=.9f))}}
   OutlinedTextField(s.input,vm::setInput,Modifier.fillMaxWidth().heightIn(min=210.dp),label={Text("Votre texte")},placeholder={Text("Écrivez ou collez votre texte ici…")},shape=RoundedCornerShape(16.dp))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(vm::correct,Modifier.weight(1f),enabled=s.input.isNotBlank()&&!s.busy,shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Blue)){Text("✓ Corriger")};Button(vm::rewrite,Modifier.weight(1f),enabled=s.input.isNotBlank()&&!s.busy,shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("✨ Reformuler")}}
   if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Blue)
   if(s.output.isNotBlank()){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=SoftBlue)){Column(Modifier.padding(16.dp)){Text("Résultat",fontWeight=FontWeight.Bold,color=DeepBlue);Spacer(Modifier.height(6.dp));Text(s.output,style=MaterialTheme.typography.bodyLarge)}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(vm::useResult){Text("Remplacer")};OutlinedButton(vm::clearOutput){Text("Effacer")}}}
   Text(s.message,style=MaterialTheme.typography.bodySmall,color=DeepBlue)
   HorizontalDivider()
   Text("⚡ Options intelligentes",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold,color=DeepBlue)
   Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=SoftRed)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Correction Internet",fontWeight=FontWeight.Bold,color=Red);Text(if(s.internetEnabled)"Internet si disponible, puis secours local." else "Aucun texte n’est envoyé en ligne.",style=MaterialTheme.typography.bodySmall)};Switch(s.internetEnabled,vm::setInternetEnabled,colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Red))}}
   Text("🎯 Style préféré",fontWeight=FontWeight.Bold,color=DeepBlue)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Standard","Professionnel","Simple","Chaleureux").forEach{style->FilterChip(selected=s.style==style,onClick={vm.setStyle(style)},label={Text(style)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=SoftBlue,selectedLabelColor=DeepBlue))}}
   HorizontalDivider()
   Text("🧠 Mémoire personnelle avancée",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold,color=DeepBlue)
   Text("Tout reste sur cet appareil : vocabulaire, mots protégés et corrections préférées.")
   OutlinedTextField(vocab,{vocab=it},Modifier.fillMaxWidth(),label={Text("Ajouter au vocabulaire")},singleLine=true,shape=RoundedCornerShape(14.dp))
   Button(onClick={if(vocab.isNotBlank()){vm.addVocabulary(vocab);vocab=""}},enabled=vocab.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Blue)){Text("➕ Mémoriser ce mot")}
   if(s.vocabulary.isNotEmpty()){Text("Vocabulaire : "+s.vocabulary.joinToString(" • "));TextButton(onClick={vm.removeVocabulary(s.vocabulary.last())}){Text("Retirer le dernier")}}
   OutlinedTextField(protected,{protected=it},Modifier.fillMaxWidth(),label={Text("Mot à ne jamais corriger")},singleLine=true,shape=RoundedCornerShape(14.dp))
   Button(onClick={if(protected.isNotBlank()){vm.addProtectedWord(protected);protected=""}},enabled=protected.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("🛡️ Protéger ce mot")}
   if(s.protectedWords.isNotEmpty()){Text("Protégés : "+s.protectedWords.joinToString(" • "));TextButton(onClick={vm.removeProtectedWord(s.protectedWords.last())}){Text("Retirer le dernier")}}
   Text("🔁 Correction préférée",fontWeight=FontWeight.Bold,color=DeepBlue)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(from,{from=it},Modifier.weight(1f),label={Text("Avant")},singleLine=true);OutlinedTextField(to,{to=it},Modifier.weight(1f),label={Text("Après")},singleLine=true)}
   Button(onClick={if(from.isNotBlank()&&to.isNotBlank()){vm.learnReplacement(from,to);from="";to=""}},enabled=from.isNotBlank()&&to.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Blue)){Text("🧠 Apprendre cette correction")}
   if(s.learnedReplacements.isNotEmpty()){Text("Préférences mémorisées :");s.learnedReplacements.entries.take(8).forEach{(a,b)->Text("• $a → $b")};TextButton(onClick={vm.removeReplacement(s.learnedReplacements.keys.last())}){Text("Oublier la dernière")}}
   HorizontalDivider()
   Text("🗂️ Historique",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold,color=DeepBlue)
   Text("Les 20 dernières corrections/reformulations restent sur l’appareil.")
   s.history.take(5).forEach{item->OutlinedButton({vm.loadHistory(item)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){Text(item.mode+" — "+item.input.take(70))}}
   Row{if(s.history.isNotEmpty())TextButton(vm::clearHistory){Text("Effacer l’historique",color=Red)};TextButton(vm::clearPersonalMemory){Text("Effacer préférences")};TextButton(vm::clearMemory){Text("Tout effacer",color=Red)}}
   HorizontalDivider()
   Text("🔒 Confidentialité",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold,color=DeepBlue)
   Text("La mémoire avancée est locale. En mode Internet, le texte peut être envoyé à LanguageTool en ligne. Désactivez Internet pour rester hors connexion.")
  }
 }}
}