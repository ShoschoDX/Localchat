package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/**
 * Generates a clean, deterministic QR-code visual representation for local peer discovery
 * with standard finder patterns (3 corner target squares) and data matrix hash encoding.
 */
@Composable
fun QrCodeVisual(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matrixSize = 25
    val grid = remember(data) {
        generateQrMatrix(data, matrixSize)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 24.dp)) {
            val cellSize = this.size.width / matrixSize

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (grid[r][c]) {
                        drawRect(
                            color = qrColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(data: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    fun drawFinder(topRow: Int, leftCol: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isCenter = r in 2..4 && c in 2..4
                matrix[topRow + r][leftCol + c] = isBorder || isCenter
            }
        }
    }

    // Draw standard 3 QR finder patterns (top-left, top-right, bottom-left)
    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // Timing patterns
    for (i in 8 until size - 8) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Alignment pattern
    val alignRow = size - 9
    val alignCol = size - 9
    for (r in -2..2) {
        for (c in -2..2) {
            val isBorder = r == -2 || r == 2 || c == -2 || c == 2
            val isDot = r == 0 && c == 0
            matrix[alignRow + r][alignCol + c] = isBorder || isDot
        }
    }

    // Deterministic payload hashing to populate data cells
    val hash = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
    var bitIndex = 0
    val totalBits = hash.size * 8

    for (r in 0 until size) {
        for (c in 0 until size) {
            // Skip finder zones
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= size - 8
            val inBottomLeft = r >= size - 8 && c < 8
            val inAlign = (r in alignRow - 2..alignRow + 2) && (c in alignCol - 2..alignCol + 2)
            val isTiming = (r == 6) || (c == 6)

            if (inTopLeft || inTopRight || inBottomLeft || inAlign || isTiming) {
                continue
            }

            val bytePos = (bitIndex / 8) % hash.size
            val bitPos = bitIndex % 8
            val bit = (hash[bytePos].toInt() shr bitPos) and 1
            matrix[r][c] = (bit == 1) xor ((r + c) % 3 == 0)
            bitIndex++
        }
    }

    return matrix
}
