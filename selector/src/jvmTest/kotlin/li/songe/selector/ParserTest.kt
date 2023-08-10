package li.songe.selector

import junit.framework.TestCase.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import li.songe.selector.parser.ParserSet
import org.junit.Test
import java.io.File


class ParserTest {

    @Test
    fun test_expression() {
        println(ParserSet.expressionParser("a>1&&b>1&&c>1||d>1", 0).data)
        println(Selector.parse("View[a>1&&b>1&&c>1||d>1&&x^=1] > Button[a>1||b*='zz'||c^=1]"))
    }

    @Test
    fun string_selector() {
        val text =
            "ImageView < @FrameLayout < LinearLayout < RelativeLayout <n LinearLayout < RelativeLayout + LinearLayout > RelativeLayout > TextView[text\$='广告']"
        println("trackIndex: " + Selector.parse(text).trackIndex)
    }

    @Test
    fun query_selector() {
        val projectCwd = File("../").absolutePath
        val text =
            "* > View[isClickable=true][childCount=1][textLen=0] > Image[isClickable=false][textLen=0]"
        val selector = Selector.parse(text)
        println("selector: $selector")

        val jsonString = File("$projectCwd/_assets/snapshot-1686629593092.json").readText()
        val json = Json {
            ignoreUnknownKeys = true
        }
        val nodes = json.decodeFromString<TestSnapshot>(jsonString).nodes

        nodes.forEach { node ->
            node.parent = nodes.getOrNull(node.pid)
            node.parent?.apply {
                children.add(node)
            }
        }
        val transform = Transform<TestNode>(getAttr = { node, name ->
            val value = node.attr[name] ?: return@Transform null
            if (value is JsonNull) return@Transform null
            value.intOrNull ?: value.booleanOrNull ?: value.content
        }, getName = { node -> node.attr["name"]?.content }, getChildren = { node ->
            node.children.asSequence()
        }, getParent = { node -> node.parent })
        val targets = transform.querySelectorAll(nodes.first(), selector).toList()
        println("target_size: " + targets.size)
        assertTrue(targets.size == 1)
        println("id: " + targets.first().id)

        val trackTargets = transform.querySelectorTrackAll(nodes.first(), selector).toList()
        println("trackTargets_size: " + trackTargets.size)
        assertTrue(trackTargets.size == 1)
        println(trackTargets.first().mapIndexed { index, testNode ->
            testNode.id to selector.tracks[index]
        })
    }

    @Test
    fun check_parser() {
        println(Selector.parse("View > Text"))
    }
}