package org.openedx.core.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors

@Composable
fun RenderHtmlContent(html: String) {
    val document = remember(html) { Jsoup.parse(html) }
    val bodyElements = document.body().children()
    Column {
        bodyElements.forEach { element ->
            RenderBlockElement(element)
        }
    }
}

@Composable
private fun RenderClickableText(annotated: AnnotatedString) {
    val context = LocalContext.current
    val hasLink = annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val modifier = if (hasLink) {
        Modifier.pointerInput(annotated) {
            detectTapGestures { offset ->
                textLayoutResult?.let { layoutResult ->
                    val position = layoutResult.getOffsetForPosition(offset)
                    annotated.getStringAnnotations("URL", position, position)
                        .firstOrNull()?.let { annotation ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                e.printStackTrace()
                            }
                        }
                }
            }
        }
    } else {
        Modifier
    }
    Text(
        text = annotated,
        modifier = modifier,
        color = MaterialTheme.appColors.textPrimary,
        onTextLayout = { textLayoutResult = it }
    )
}

@Composable
private fun RenderParagraph(element: Element) {
    val segments = extractSegmentsFromNodes(element.childNodes())
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is List<*> -> {
                    val nodes = segment.filterIsInstance<Node>()
                    val annotated = buildAnnotatedStringFromNodes(nodes)
                    RenderClickableText(annotated)
                }

                is Element -> {
                    RenderBlockElement(segment)
                }
            }
        }
    }
}

private fun extractSegmentsFromNodes(nodes: List<Node>): List<Any> {
    val segments = mutableListOf<Any>()
    val currentSegment = mutableListOf<Node>()

    for (node in nodes) {
        if (node is Element) {
            val tagName = node.tagName()
            if (tagName == "img" || tagName == "ul" || tagName == "ol" || tagName == "blockquote") {
                flush(currentSegment, segments)
                segments.add(node)
            } else if (node.select("img").isNotEmpty()) {
                flush(currentSegment, segments)
                segments.addAll(extractSegmentsFromNodes(node.childNodes()))
            } else {
                currentSegment.add(node)
            }
        } else {
            currentSegment.add(node)
        }
    }
    flush(currentSegment, segments)
    return segments
}

@Composable
private fun RenderBlockElement(element: Element, indent: Int = 0) {
    when (element.tagName()) {
        "p" -> {
            RenderParagraph(element)
        }

        "ul" -> {
            Column(modifier = Modifier.padding(start = (indent + 1) * 16.dp)) {
                element.children().forEach { child ->
                    if (child.tagName() == "li") {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = AnnotatedString("• "),
                                style = TextStyle(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.appColors.textPrimary
                            )
                            RenderBlockElement(child, indent + 1)
                        }
                    }
                }
            }
        }

        "ol" -> {
            Column(modifier = Modifier.padding(start = (indent + 1) * 16.dp)) {
                element.children().forEachIndexed { index, child ->
                    if (child.tagName() == "li") {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = AnnotatedString("${index + 1}. "),
                                color = MaterialTheme.appColors.textPrimary
                            )
                            RenderBlockElement(child, indent + 1)
                        }
                    }
                }
            }
        }

        "li" -> {
            RenderParagraph(element)
        }

        "blockquote" -> {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.appColors.cardViewBorder)
                )
                Column {
                    element.children().forEach { child ->
                        RenderBlockElement(child)
                    }
                }
            }
        }

        "img" -> {
            val src = element.attr("src")
            AsyncImage(
                modifier = Modifier.fillMaxWidth(),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(src)
                    .error(R.drawable.core_no_image_course)
                    .placeholder(R.drawable.core_no_image_course)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        }

        else -> {
            RenderParagraph(element)
        }
    }
}

@Composable
private fun AnnotatedString.Builder.AppendNodes(nodes: List<Node>) {
    nodes.forEach { node ->
        when (node) {
            is TextNode -> append(node.text())
            is Element -> AppendElement(node)
        }
    }
}

@Composable
private fun AnnotatedString.Builder.AppendElement(element: Element) {
    when (element.tagName()) {
        "br" -> append("\n")
        "strong" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            AppendNodes(element.childNodes())
        }

        "em" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            AppendNodes(element.childNodes())
        }

        "code" -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            AppendNodes(element.childNodes())
        }

        "span" -> {
            val styleAttr = element.attr("style")
            if (styleAttr.contains("text-decoration: underline", ignoreCase = true)) {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    AppendNodes(element.childNodes())
                }
            } else {
                AppendNodes(element.childNodes())
            }
        }

        "a" -> {
            val href = element.attr("href")
            val start = this.length
            AppendNodes(element.childNodes())
            val end = this.length
            addStyle(
                SpanStyle(
                    color = MaterialTheme.appColors.primary,
                    textDecoration = TextDecoration.Underline
                ),
                start,
                end
            )
            addStringAnnotation(tag = "URL", annotation = href, start = start, end = end)
        }

        else -> AppendNodes(element.childNodes())
    }
}

@Composable
private fun buildAnnotatedStringFromNodes(nodes: List<Node>): AnnotatedString {
    return AnnotatedString.Builder().apply {
        AppendNodes(nodes)
    }.toAnnotatedString()
}

private fun flush(currentSegment: MutableList<Node>, segments: MutableList<Any>) {
    if (currentSegment.isNotEmpty()) {
        segments.add(currentSegment.toList())
        currentSegment.clear()
    }
}
