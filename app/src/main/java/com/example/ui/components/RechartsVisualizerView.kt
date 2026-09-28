package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RechartsCashFlowWebView(
    data: List<DailyCashFlowPoint>,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(data) {
        buildRechartsHtml(data)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(14.dp)),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(0) // Transparent
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html; charset=UTF-8", "UTF-8", null)
        }
    )
}

private fun buildRechartsHtml(data: List<DailyCashFlowPoint>): String {
    val labelsJson = data.joinToString(",") { "\"${it.dateLabel}\"" }
    val fullDatesJson = data.joinToString(",") { "\"${it.fullDate}\"" }
    val incomeJson = data.joinToString(",") { it.income.toString() }
    val expenseJson = data.joinToString(",") { it.expense.toString() }
    val netJson = data.joinToString(",") { (it.income - it.expense).toString() }

    return """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                body { background: transparent; padding: 10px; color: #1f2937; direction: rtl; }
                .chart-container { background: #ffffff; border-radius: 12px; padding: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
                .chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
                .chart-title { font-size: 13px; font-weight: bold; color: #0D6B42; }
                .legend-container { display: flex; gap: 12px; font-size: 11px; }
                .legend-item { display: flex; align-items: center; gap: 5px; }
                .legend-dot { width: 10px; height: 10px; border-radius: 50%; }
                .dot-income { background: #1B8755; }
                .dot-expense { background: #D32F2F; }
                .dot-net { background: #1976D2; border-radius: 2px; height: 3px; }
                
                .svg-container { width: 100%; height: 160px; position: relative; }
                svg { width: 100%; height: 100%; overflow: visible; }
                
                .bar-income { fill: url(#incomeGradient); transition: all 0.3s; cursor: pointer; }
                .bar-expense { fill: url(#expenseGradient); transition: all 0.3s; cursor: pointer; }
                .bar-income:hover, .bar-income:active { filter: brightness(1.15); }
                .bar-expense:hover, .bar-expense:active { filter: brightness(1.15); }
                
                .tooltip {
                    position: absolute; display: none; background: rgba(17, 24, 39, 0.92);
                    color: white; padding: 6px 10px; border-radius: 6px; font-size: 11px;
                    pointer-events: none; transform: translate(-50%, -100%); white-space: nowrap; z-index: 10;
                }
            </style>
        </head>
        <body>
            <div class="chart-container">
                <div class="chart-header">
                    <span class="chart-title">مخطط التدفق النقدي اليومي (Recharts Engine)</span>
                    <div class="legend-container">
                        <div class="legend-item"><span class="legend-dot dot-income"></span><span>دخل (+)</span></div>
                        <div class="legend-item"><span class="legend-dot dot-expense"></span><span>صرف (-)</span></div>
                        <div class="legend-item"><span class="legend-dot dot-net"></span><span>صافي</span></div>
                    </div>
                </div>
                <div class="svg-container" id="svgContainer">
                    <div class="tooltip" id="tooltip"></div>
                    <svg id="chartSvg">
                        <defs>
                            <linearGradient id="incomeGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stop-color="#1B8755"/>
                                <stop offset="100%" stop-color="#81C784"/>
                            </linearGradient>
                            <linearGradient id="expenseGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stop-color="#D32F2F"/>
                                <stop offset="100%" stop-color="#EF9A9A"/>
                            </linearGradient>
                        </defs>
                        <g id="gridLines"></g>
                        <g id="barsGroup"></g>
                        <g id="netLineGroup"></g>
                        <g id="labelsGroup"></g>
                    </svg>
                </div>
            </div>

            <script>
                const labels = [$labelsJson];
                const fullDates = [$fullDatesJson];
                const incomeData = [$incomeJson];
                const expenseData = [$expenseJson];
                const netData = [$netJson];

                const container = document.getElementById('svgContainer');
                const svg = document.getElementById('chartSvg');
                const tooltip = document.getElementById('tooltip');

                function renderChart() {
                    const width = container.clientWidth || 340;
                    const height = 135;
                    const count = labels.length || 1;
                    const groupWidth = width / count;
                    const barWidth = Math.min(groupWidth * 0.32, 22);

                    const maxVal = Math.max(...incomeData, ...expenseData, 100) * 1.15;

                    // Draw grid lines
                    const gridGroup = document.getElementById('gridLines');
                    gridGroup.innerHTML = '';
                    for (let i = 0; i <= 3; i++) {
                        const y = (height / 3) * i;
                        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                        line.setAttribute('x1', 0);
                        line.setAttribute('y1', y);
                        line.setAttribute('x2', width);
                        line.setAttribute('y2', y);
                        line.setAttribute('stroke', '#e5e7eb');
                        line.setAttribute('stroke-dasharray', '3,3');
                        gridGroup.appendChild(line);
                    }

                    // Draw Bars & Line
                    const barsGroup = document.getElementById('barsGroup');
                    const netGroup = document.getElementById('netLineGroup');
                    const labelsGroup = document.getElementById('labelsGroup');
                    barsGroup.innerHTML = '';
                    netGroup.innerHTML = '';
                    labelsGroup.innerHTML = '';

                    let linePoints = [];

                    for (let i = 0; i < count; i++) {
                        const centerX = groupWidth * i + groupWidth / 2;
                        const inc = incomeData[i] || 0;
                        const exp = expenseData[i] || 0;
                        const net = netData[i] || 0;

                        const incH = (inc / maxVal) * height;
                        const expH = (exp / maxVal) * height;

                        // Income Bar
                        if (incH > 0) {
                            const barInc = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
                            barInc.setAttribute('class', 'bar-income');
                            barInc.setAttribute('x', centerX - barWidth - 2);
                            barInc.setAttribute('y', height - incH);
                            barInc.setAttribute('width', barWidth);
                            barInc.setAttribute('height', incH);
                            barInc.setAttribute('rx', 3);
                            attachTooltip(barInc, fullDates[i], inc, exp, net);
                            barsGroup.appendChild(barInc);
                        }

                        // Expense Bar
                        if (expH > 0) {
                            const barExp = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
                            barExp.setAttribute('class', 'bar-expense');
                            barExp.setAttribute('x', centerX + 2);
                            barExp.setAttribute('y', height - expH);
                            barExp.setAttribute('width', barWidth);
                            barExp.setAttribute('height', expH);
                            barExp.setAttribute('rx', 3);
                            attachTooltip(barExp, fullDates[i], inc, exp, net);
                            barsGroup.appendChild(barExp);
                        }

                        // Net point
                        const netY = height - Math.max(0, (net / maxVal) * height);
                        linePoints.push({ x: centerX, y: Math.min(height, Math.max(4, netY)) });

                        // Text label
                        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                        text.setAttribute('x', centerX);
                        text.setAttribute('y', height + 18);
                        text.setAttribute('text-anchor', 'middle');
                        text.setAttribute('font-size', '10');
                        text.setAttribute('fill', '#6b7280');
                        text.textContent = labels[i];
                        labelsGroup.appendChild(text);
                    }

                    // Net Flow path
                    if (linePoints.length > 1) {
                        let pathD = 'M ' + linePoints[0].x + ' ' + linePoints[0].y;
                        for (let j = 1; j < linePoints.length; j++) {
                            pathD += ' L ' + linePoints[j].x + ' ' + linePoints[j].y;
                        }
                        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                        path.setAttribute('d', pathD);
                        path.setAttribute('fill', 'none');
                        path.setAttribute('stroke', '#1976D2');
                        path.setAttribute('stroke-width', '2.5');
                        netGroup.appendChild(path);

                        linePoints.forEach(function(pt) {
                            const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                            circle.setAttribute('cx', pt.x);
                            circle.setAttribute('cy', pt.y);
                            circle.setAttribute('r', '3');
                            circle.setAttribute('fill', '#1976D2');
                            circle.setAttribute('stroke', '#ffffff');
                            circle.setAttribute('stroke-width', '1.5');
                            netGroup.appendChild(circle);
                        });
                    }
                }

                function attachTooltip(elem, date, inc, exp, net) {
                    const show = function(e) {
                        const rect = container.getBoundingClientRect();
                        const clientX = e.clientX || (e.touches && e.touches[0].clientX) || 0;
                        const clientY = e.clientY || (e.touches && e.touches[0].clientY) || 0;
                        tooltip.style.left = (clientX - rect.left) + 'px';
                        tooltip.style.top = (clientY - rect.top - 10) + 'px';
                        tooltip.style.display = 'block';
                        tooltip.innerHTML = '<strong>' + date + '</strong><br>دخل: +' + inc.toLocaleString() + ' ر.س<br>صرف: -' + exp.toLocaleString() + ' ر.س<br>صافي: ' + net.toLocaleString() + ' ر.س';
                    };
                    elem.addEventListener('mouseenter', show);
                    elem.addEventListener('touchstart', show);
                    elem.addEventListener('mouseleave', function() { tooltip.style.display = 'none'; });
                }

                window.addEventListener('resize', renderChart);
                window.addEventListener('DOMContentLoaded', renderChart);
                renderChart();
            </script>
        </body>
        </html>
    """.trimIndent()
}
