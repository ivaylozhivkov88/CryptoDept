package com.cryptodept.ui.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.education.GlossaryDatabase.GlossaryCategory
import com.cryptodept.domain.education.GlossaryDatabase.GlossaryEntry
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.GlossaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(
    viewModel: GlossaryViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val query by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val entries by viewModel.filteredEntries.collectAsState()
    val colors = LocalTerminalColors.current
    
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        
        Text(
            text = ">>> CRYPTO_GLOSSARY",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp),
        )
        
        Text(
            text = "${entries.size} terms — Learn the crypto language",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateSearch,
            placeholder = { 
                Text("Search terms...", fontFamily = FontFamily.Monospace, fontSize = 12.sp) 
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.grid,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            singleLine = true,
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CategoryChip(
                    label = "ALL",
                    isSelected = selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                )
            }
            items(GlossaryCategory.entries) { cat ->
                CategoryChip(
                    label = "${cat.emoji} ${cat.displayName}",
                    isSelected = selectedCategory == cat,
                    onClick = { viewModel.selectCategory(cat) },
                )
            }
        }
        
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No terms found.\nTry a different search.",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    GlossaryEntryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    
    Surface(
        color = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.background,
        border = BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.grid,
        ),
        shape = RectangleShape,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.primary else colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun GlossaryEntryCard(entry: GlossaryEntry) {
    val colors = LocalTerminalColors.current
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.grid),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = entry.category.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.term,
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (expanded) "[-]" else "[+]",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = entry.shortDefinition,
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = entry.fullExplanation,
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
                
                entry.example?.let { example ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Example: $example",
                        color = colors.amber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }
                
                if (entry.relatedIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Related: ${entry.relatedIds.joinToString(", ")}",
                        color = colors.dimText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
