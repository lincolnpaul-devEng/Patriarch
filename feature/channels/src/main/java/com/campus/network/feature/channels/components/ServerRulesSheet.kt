package com.campus.network.feature.channels.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.ServerRule

/**
 * Pixel-Perfect Server Rules & Code of Conduct Management Sheet.
 * Displays formal server guidelines and allows server owners/moderators
 * to add, edit, and delete rules.
 *
 * ZERO EMOJI POLICY: Strictly vector shields, gavels, and clean typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerRulesSheet(
    serverName: String,
    rules: List<ServerRule>,
    canManageRules: Boolean = true,
    onDismiss: () -> Unit,
    onAddRule: (title: String, description: String) -> Unit,
    onDeleteRule: (ruleId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isAddingRule by remember { mutableStateOf(false) }
    var newRuleTitle by remember { mutableStateOf("") }
    var newRuleDescription by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header: Close 'X'
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Rules",
                            tint = Color(0xFF5865F2),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Server Rules",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F22)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1E1F22),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$serverName Code of Conduct",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5865F2)
            )

            Text(
                text = "These rules are set by the server moderators to ensure a productive and safe campus environment.",
                fontSize = 13.5.sp,
                color = Color(0xFF5C6470),
                lineHeight = 18.5.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

            Spacer(modifier = Modifier.height(16.dp))

            // RULES LIST
            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = Color(0xFF949BA4),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No custom rules added yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF5C6470)
                        )
                        Text(
                            text = "Add rules to guide member behavior and discussions.",
                            fontSize = 13.sp,
                            color = Color(0xFF949BA4),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rules.forEachIndexed { index, rule ->
                        RuleCard(
                            ruleNumber = index + 1,
                            title = rule.title,
                            description = rule.description,
                            canDelete = canManageRules,
                            onDelete = { onDeleteRule(rule.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ADD RULE FORM (For Owners/Moderators)
            if (canManageRules) {
                AnimatedVisibility(
                    visible = isAddingRule,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF5865F2), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8F9FA)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Create New Rule",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1F22)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "RULE TITLE",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4E5058)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White
                            ) {
                                BasicTextField(
                                    value = newRuleTitle,
                                    onValueChange = { newRuleTitle = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 14.5.sp,
                                        color = Color(0xFF1E1F22),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (newRuleTitle.isEmpty()) {
                                                Text(
                                                    text = "e.g. Keep Discussions Course-Relevant",
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF949BA4)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "RULE DETAILS & GUIDELINES",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4E5058)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White
                            ) {
                                BasicTextField(
                                    value = newRuleDescription,
                                    onValueChange = { newRuleDescription = it },
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF1E1F22),
                                        fontWeight = FontWeight.Normal
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            if (newRuleDescription.isEmpty()) {
                                                Text(
                                                    text = "Explain the expectation or policy clearly for all members...",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF949BA4)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isAddingRule = false
                                        newRuleTitle = ""
                                        newRuleDescription = ""
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Cancel", fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = {
                                        if (newRuleTitle.isNotBlank()) {
                                            onAddRule(newRuleTitle.trim(), newRuleDescription.trim())
                                            newRuleTitle = ""
                                            newRuleDescription = ""
                                            isAddingRule = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Save Rule", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!isAddingRule) {
                    OutlinedButton(
                        onClick = { isAddingRule = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF5865F2),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Rule",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5865F2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1F22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "I Understand & Agree",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RuleCard(
    ruleNumber: Int,
    title: String,
    description: String,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rule Number Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEEF2FF)
                ) {
                    Text(
                        text = "RULE $ruleNumber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF5865F2),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Rule",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1F22)
            )

            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.5.sp,
                    color = Color(0xFF5C6470),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
