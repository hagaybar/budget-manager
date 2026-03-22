package com.budgetmanager.app.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// Card colors
private val CardDark1 = Color(0xFF1a1a2e)
private val CardDark2 = Color(0xFF16213e)
private val CardDark3 = Color(0xFF0f3460)
private val IncGreen = Color(0xFF6ee7b7)
private val ExpRed = Color(0xFFfca5a5)

@Composable
fun ShareCardDialog(
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var accountName by rememberSaveable { mutableStateOf("") }

    val totalIncome = transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val totalExpenses = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val balance = totalIncome - totalExpenses
    val recent = transactions.take(10)
    val now = LocalDate.now()
    val monthYear = now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column {
                // Card preview
                ShareCardPreview(
                    balance = balance,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    transactions = recent,
                    monthYear = monthYear,
                    onAccountNameChange = { accountName = it },
                    rawAccountName = accountName,
                )

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            shareCardImage(
                                context = context,
                                accountName = accountName.ifBlank { "My Budget" },
                                balance = balance,
                                totalIncome = totalIncome,
                                totalExpenses = totalExpenses,
                                transactions = recent,
                                monthYear = monthYear,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareCardPreview(
    balance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    transactions: List<Transaction>,
    monthYear: String,
    onAccountNameChange: (String) -> Unit,
    rawAccountName: String,
) {
    val balanceColor = if (balance >= 0) IncGreen else ExpRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CardDark1, CardDark2, CardDark3),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Account overview label
        Text(
            text = "ACCOUNT OVERVIEW",
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            ),
            color = Color.White.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Editable account name
        BasicTextField(
            value = rawAccountName,
            onValueChange = { if (it.length <= 40) onAccountNameChange(it) },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (rawAccountName.isEmpty()) {
                        Text(
                            text = "My Budget",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Balance label
        Text(
            text = "CURRENT BALANCE",
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = Color.White.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Balance amount
        val sign = if (balance < 0) "-" else ""
        Text(
            text = "$sign\u20AA${formatAmt(abs(balance))}",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            ),
            color = balanceColor,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Income / Expense pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryPill(
                label = "INCOME",
                value = "\u20AA${formatAmt(totalIncome)}",
                valueColor = IncGreen,
                modifier = Modifier.weight(1f),
            )
            SummaryPill(
                label = "EXPENSES",
                value = "\u20AA${formatAmt(totalExpenses)}",
                valueColor = ExpRed,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Recent transactions header
        Text(
            text = "RECENT TRANSACTIONS",
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = Color.White.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Transaction list
        if (transactions.isEmpty()) {
            Text(
                text = "No transactions yet",
                style = TextStyle(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.4f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
        } else {
            transactions.forEach { tx ->
                ShareTxRow(tx)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Budget Manager",
                style = TextStyle(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.4f),
            )
            Text(
                text = monthYear,
                style = TextStyle(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            ),
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShareTxRow(tx: Transaction) {
    val isIncome = tx.type == TransactionType.INCOME
    val dotColor = if (isIncome) IncGreen else ExpRed
    val prefix = if (isIncome) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Category
        Text(
            text = tx.category,
            style = TextStyle(fontSize = 13.sp),
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Amount
        Text(
            text = "$prefix\u20AA${formatAmt(tx.amount)}",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = dotColor,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Bitmap rendering + sharing via Intent
// ═══════════════════════════════════════════════════════════════════════════════

private fun shareCardImage(
    context: Context,
    accountName: String,
    balance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    transactions: List<Transaction>,
    monthYear: String,
) {
    val bitmap = renderCardBitmap(
        accountName, balance, totalIncome, totalExpenses, transactions, monthYear
    )

    // Save to cache
    val dir = File(context.cacheDir, "share_images")
    dir.mkdirs()
    val file = File(dir, "budget_card.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TEXT,
            "Balance: ${if (balance < 0) "-" else ""}\u20AA${formatAmt(abs(balance))} | " +
                "Income: \u20AA${formatAmt(totalIncome)} | " +
                "Expenses: \u20AA${formatAmt(totalExpenses)}"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share Budget Card"))
}

private fun renderCardBitmap(
    accountName: String,
    balance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    transactions: List<Transaction>,
    monthYear: String,
): Bitmap {
    val w = 1080
    val txCount = transactions.size.coerceAtMost(10)
    val h = 580 + (txCount * 60) + (if (txCount == 0) 50 else 0)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    val px = 60f // horizontal padding

    // Background gradient
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFF1a1a2e.toInt(), 0xFF16213e.toInt(), 0xFF0f3460.toInt()),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 40f, 40f, bgPaint)

    // Decorative radial glow
    val glow1 = Paint().apply {
        shader = RadialGradient(
            w * 0.8f, h * 0.1f, 350f,
            0x4D6366F1.toInt(), 0x00000000, Shader.TileMode.CLAMP,
        )
    }
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow1)
    val glow2 = Paint().apply {
        shader = RadialGradient(
            w * 0.15f, h * 0.85f, 300f,
            0x3322D3EE.toInt(), 0x00000000, Shader.TileMode.CLAMP,
        )
    }
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow2)

    val sans = Typeface.create("sans-serif", Typeface.NORMAL)
    val sansBold = Typeface.create("sans-serif", Typeface.BOLD)
    val sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    var y = 80f

    // "ACCOUNT OVERVIEW"
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99FFFFFF.toInt()
        textSize = 28f
        typeface = sansMedium
        letterSpacing = 0.12f
    }
    c.drawText("ACCOUNT OVERVIEW", px, y, labelPaint)
    y += 56f

    // Account name
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 52f
        typeface = sansBold
    }
    c.drawText(accountName, px, y, namePaint)
    y += 72f

    // "CURRENT BALANCE"
    labelPaint.textSize = 26f
    c.drawText("CURRENT BALANCE", px, y, labelPaint)
    y += 16f

    // Balance value
    val sign = if (balance < 0) "-" else ""
    val balText = "$sign\u20AA${formatAmt(abs(balance))}"
    val balPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (balance >= 0) 0xFF6ee7b7.toInt() else 0xFFfca5a5.toInt()
        textSize = 84f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        letterSpacing = -0.02f
    }
    c.drawText(balText, px, y + 76f, balPaint)
    y += 108f

    // Income / Expense boxes
    val boxW = (w - px * 2 - 24f) / 2
    val boxH = 130f
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1AFFFFFF
    }
    val boxRect1 = RectF(px, y, px + boxW, y + boxH)
    c.drawRoundRect(boxRect1, 24f, 24f, boxPaint)
    val boxRect2 = RectF(px + boxW + 24f, y, px + boxW * 2 + 24f, y + boxH)
    c.drawRoundRect(boxRect2, 24f, 24f, boxPaint)

    // Income box text
    val boxLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt()
        textSize = 24f
        typeface = sansMedium
        letterSpacing = 0.08f
    }
    c.drawText("INCOME", px + 28f, y + 42f, boxLabelPaint)
    val incValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6ee7b7.toInt()
        textSize = 38f
        typeface = sansBold
    }
    c.drawText("\u20AA${formatAmt(totalIncome)}", px + 28f, y + 92f, incValPaint)

    // Expense box text
    c.drawText("EXPENSES", px + boxW + 52f, y + 42f, boxLabelPaint)
    val expValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFfca5a5.toInt()
        textSize = 38f
        typeface = sansBold
    }
    c.drawText("\u20AA${formatAmt(totalExpenses)}", px + boxW + 52f, y + 92f, expValPaint)

    y += boxH + 40f

    // Divider
    val divPaint = Paint().apply { color = 0x26FFFFFF }
    c.drawRect(px, y, w - px, y + 2f, divPaint)
    y += 36f

    // "RECENT TRANSACTIONS"
    labelPaint.textSize = 26f
    c.drawText("RECENT TRANSACTIONS", px, y, labelPaint)
    y += 32f

    // Transaction rows
    if (transactions.isEmpty()) {
        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF
            textSize = 34f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
        }
        c.drawText("No transactions yet", px, y + 34f, emptyPaint)
    } else {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt()
            textSize = 34f
            typeface = sans
        }
        val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f
            typeface = sansMedium
            textAlign = Paint.Align.RIGHT
        }

        transactions.take(10).forEach { tx ->
            val isInc = tx.type == TransactionType.INCOME
            dotPaint.color = if (isInc) 0xFF6ee7b7.toInt() else 0xFFfca5a5.toInt()
            c.drawCircle(px + 8f, y + 20f, 8f, dotPaint)

            // Truncate category if too long
            val maxCatW = w - px * 2 - 280f
            var cat = tx.category
            while (catPaint.measureText(cat) > maxCatW && cat.length > 1) {
                cat = cat.dropLast(1)
            }
            if (cat != tx.category) cat += "..."
            c.drawText(cat, px + 32f, y + 28f, catPaint)

            val prefix = if (isInc) "+" else "-"
            amtPaint.color = dotPaint.color
            c.drawText("$prefix\u20AA${formatAmt(tx.amount)}", w - px, y + 28f, amtPaint)

            y += 60f
        }
    }

    // Footer divider
    y += 8f
    divPaint.color = 0x14FFFFFF
    c.drawRect(px, y, w - px, y + 2f, divPaint)
    y += 36f

    // Footer
    val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80FFFFFF.toInt()
        textSize = 26f
        typeface = sans
    }
    c.drawText("Budget Manager", px, y, footPaint)
    footPaint.textAlign = Paint.Align.RIGHT
    c.drawText(monthYear, w - px, y, footPaint)

    return bitmap
}

private fun formatAmt(value: Double): String {
    return String.format(Locale.US, "%,.2f", value)
}
