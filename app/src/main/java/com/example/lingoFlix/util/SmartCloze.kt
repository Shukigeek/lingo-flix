package com.example.lingoFlix.util

import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * On-device "smart cloze" engine (a port of the backend's heuristic engine).
 *
 * Instead of hiding random words it scores every token for learning value
 * (content words > filler, longer > shorter, avoids proper nouns/contractions),
 * estimates CEFR difficulty and grades answers with fuzzy matching.
 */
object SmartCloze {

    data class Cloze(
        val tokens: List<String>,
        val hiddenIndices: List<Int>,
        val hiddenWords: List<String>,
        val choices: List<List<String>> = emptyList(),
        val hint: String? = null,
        val cefr: String = "A2"
    )

    data class WordGrade(val word: String, val correct: Boolean, val userWord: String?, val similarity: Double)
    data class Grade(val allCorrect: Boolean, val similarity: Double, val perWord: List<WordGrade>)

    private val tokenSplit = Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?;:\"“”()])|(?=[.,!?;:\"“”()])")
    private val punctuation = setOf(".", ",", "!", "?", ";", ":", "\"", "“", "”", "(", ")")

    private val stopEn = """a an the and or but if so of to in on at by for with from as is are was were be been being am
        do does did have has had it its this that these those i you he she we they me him her us them my your his
        our their what which who whom whose where when why how not no yes oh ok okay um uh hey there here then than
        too very just also can could will would shall should may might must about up down out over under again
        into onto off all any some each few more most other such only own same s t don ll re ve d m""".split(Regex("\\s+")).toSet()
    private val stopEs = "el la los las un una unos unas de del a al y o pero si no que en con por para es son está están yo tú él ella nosotros ellos mi tu su lo le se me te nos".split(" ").toSet()
    private val stopFr = "le la les un une des de du à au aux et ou mais si ne pas que qui en dans sur avec pour est sont je tu il elle nous vous ils elles ce cette ces mon ma mes ton ta tes son sa ses".split(" ").toSet()
    private val stopDe = "der die das ein eine einer eines und oder aber wenn nicht ja nein zu in auf mit für von ist sind ich du er sie es wir ihr mein dein sein".split(" ").toSet()
    private val stopHe = "של את על אני אתה הוא היא אנחנו הם הן זה זאת לא כן מה מי איך למה גם רק עם כל יש אין אבל או אז כי".split(" ").toSet()

    private val boostEn = setOf(
        "break", "pivot", "moist", "awkward", "gorgeous", "hilarious", "obviously", "seriously", "apparently",
        "actually", "literally", "ridiculous", "unbelievable", "definitely", "probably", "whatever", "anyway",
        "mean", "guess", "suppose", "figure", "kidding", "freak", "chill", "hang", "grab", "dump", "crush",
        "date", "weird", "creepy", "gross", "fancy", "cheap", "broke", "wedding", "divorce"
    )

    private val cefrA1 = """hello hi bye good bad yes no please thank thanks sorry name friend family mother father sister brother
        house home school work day night morning time today tomorrow yesterday week year food water coffee tea
        eat drink go come see look like love want need have make take give get know think say tell talk ask
        big small new old happy sad hot cold one two three four five six seven eight nine ten first last
        man woman boy girl people person child city country car bus train phone book money job door room""".split(Regex("\\s+")).toSet()
    private val cefrA2 = """always never sometimes often usually already still yet again enough maybe because although while
        beautiful dangerous difficult easy expensive famous favorite important interesting popular quiet ready
        strange terrible wonderful decide forget remember explain arrive leave stay wait wear carry choose
        follow invite miss prefer promise return travel visit worry married single boyfriend girlfriend
        kitchen bedroom apartment restaurant office hospital airport ticket weather holiday birthday party""".split(Regex("\\s+")).toSet()
    private val cefrB1 = """actually apparently basically definitely eventually exactly honestly obviously particularly probably
        seriously suddenly unfortunately admit afford apologize appreciate argue avoid complain convince deserve
        disappoint embarrass encourage ignore insist mention pretend realize recognize refuse regret relax
        suggest suppose survive threaten warn awkward confident curious embarrassed guilty jealous nervous
        proud relieved ridiculous responsible upset relationship situation opportunity experience attitude""".split(Regex("\\s+")).toSet()

    fun stopwords(lang: String): Set<String> = when (lang) {
        "es" -> stopEs; "fr" -> stopFr; "de" -> stopDe; "he" -> stopHe; else -> stopEn
    }

    fun tokenize(text: String): List<String> = text.split(tokenSplit).filter { it.isNotBlank() }

    fun cleanWord(w: String): String = w.lowercase().trim().replace(Regex("^[^\\p{L}\\p{N}']+|[^\\p{L}\\p{N}']+$"), "")

    fun stripAccents(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    fun wordValue(word: String, lang: String, position: Int, total: Int): Double {
        val w = cleanWord(word)
        if (w.isEmpty() || w.none { it.isLetter() }) return 0.0
        if (w in stopwords(lang)) return 0.08
        var score = 0.35
        score += min(w.length, 10) / 25.0
        if (lang == "en" && w in boostEn) score += 0.3
        score += when {
            w in cefrA1 -> -0.1
            w in cefrA2 -> 0.05
            w in cefrB1 -> 0.15
            else -> 0.12
        }
        if (word.firstOrNull()?.isUpperCase() == true && position > 0) score -= 0.2
        if ("'" in w) score -= 0.1
        if (total > 3) {
            val rel = position.toDouble() / max(total - 1, 1)
            score += 0.08 * (1 - abs(rel - 0.5) * 2)
        }
        return score.coerceIn(0.0, 1.0)
    }

    fun estimateCefr(text: String, lang: String = "en"): Pair<String, Double> {
        val words = tokenize(text).map { cleanWord(it) }.filter { it.isNotEmpty() && it.any { c -> c.isLetter() } }
        if (words.isEmpty()) return "A1" to 0.0
        val n = words.size
        val avgLen = words.sumOf { it.length }.toDouble() / n
        var hard = 0
        var unknown = 0
        val stops = stopwords(lang)
        for (w in words) {
            when {
                w in stops || w in cefrA1 -> {}
                w in cefrA2 -> {}
                w in cefrB1 -> hard++
                else -> unknown++
            }
        }
        val ratioHard = (hard + unknown * 1.4) / n
        val lengthFactor = min(n / 18.0, 1.0)
        val complexity = (0.45 * ratioHard + 0.3 * lengthFactor + 0.25 * min((avgLen - 3) / 5, 1.0)).coerceIn(0.0, 1.0)
        val cefr = when {
            complexity < 0.18 -> "A1"; complexity < 0.32 -> "A2"; complexity < 0.48 -> "B1"
            complexity < 0.65 -> "B2"; complexity < 0.8 -> "C1"; else -> "C2"
        }
        return cefr to complexity
    }

    private fun hiddenCount(candidates: Int, difficulty: String, mode: String): Int {
        if (mode == "multiple_choice" || mode == "listening") return 1
        return when (difficulty) {
            "קשה", "hard" -> max(1, ceil(candidates * 0.7).toInt())
            "בינוני", "medium" -> max(1, ceil(candidates * 0.4).toInt())
            else -> 1
        }
    }

    fun build(text: String, difficulty: String = "easy", mode: String = "typing", lang: String = "en", seed: Long? = null): Cloze {
        val rng = Random(seed ?: text.hashCode().toLong())
        val tokens = tokenize(text)
        val n = tokens.size
        val scored = tokens.mapIndexed { i, t -> wordValue(t, lang, i, n) to i }
        var candidates = scored.filter { it.first > 0.1 }
        if (candidates.isEmpty()) candidates = scored.filter { it.first > 0 }
        val (cefr, _) = estimateCefr(text, lang)
        if (candidates.isEmpty()) return Cloze(tokens, emptyList(), emptyList(), cefr = cefr)

        val k = min(hiddenCount(candidates.size, difficulty, mode), candidates.size)
        val chosen = mutableListOf<Int>()
        val pool = candidates.toMutableList()
        while (pool.isNotEmpty() && chosen.size < k) {
            val weights = pool.map { max(it.first, 0.01).let { s -> s * s } }
            var r = rng.nextDouble() * weights.sum()
            var pick = 0
            for ((i, w) in weights.withIndex()) {
                r -= w
                if (r <= 0) { pick = i; break }
                pick = i
            }
            chosen.add(pool[pick].second)
            pool.removeAt(pick)
        }
        var finalChosen = chosen.sorted()
        if (difficulty != "hard" && difficulty != "קשה") {
            val filtered = mutableListOf<Int>()
            for (idx in finalChosen) if (filtered.isEmpty() || idx - filtered.last() > 1) filtered.add(idx)
            if (filtered.isNotEmpty()) finalChosen = filtered
        }
        val hiddenWords = finalChosen.map { tokens[it] }
        val choices = if (mode == "multiple_choice") {
            val poolWords = tokens.filter { it !in hiddenWords }
            hiddenWords.map { makeChoices(it, poolWords, lang, rng) }
        } else emptyList()
        val hint = if ((difficulty == "easy" || difficulty == "קל") && hiddenWords.isNotEmpty()) makeHint(hiddenWords.first()) else null
        return Cloze(tokens, finalChosen, hiddenWords, choices, hint, cefr)
    }

    fun makeHint(word: String): String {
        val w = cleanWord(word)
        if (w.isEmpty()) return ""
        if (w.length <= 3) return w.first() + "_".repeat(w.length - 1)
        return "${w.first()}${"_".repeat(w.length - 2)}${w.last()}  (${w.length})"
    }

    private fun mutate(word: String, rng: Random): String {
        if (word.length < 3) return word + "s"
        return when (rng.nextInt(4)) {
            0 -> {
                val endings = listOf("ing" to "ed", "ed" to "ing", "s" to "", "ly" to "", "y" to "ies", "e" to "ing")
                endings.firstOrNull { word.endsWith(it.first) }?.let { word.dropLast(it.first.length) + it.second } ?: (word + "s")
            }
            1 -> if (word.length > 3) { val i = rng.nextInt(1, word.length - 2); word.substring(0, i) + word[i + 1] + word[i] + word.substring(i + 2) } else word + "s"
            2 -> { val i = rng.nextInt(1, word.length - 1); word.substring(0, i) + word[i] + word.substring(i) }
            else -> word.dropLast(1)
        }
    }

    fun makeChoices(word: String, pool: List<String>, lang: String, rng: Random, k: Int = 4): List<String> {
        val target = cleanWord(word)
        val stops = stopwords(lang)
        val candidates = pool.map { cleanWord(it) }
            .filter { it.isNotEmpty() && it != target && it !in stops && abs(it.length - target.length) <= 2 }
            .distinct().shuffled(rng)
        val distractors = candidates.take(k - 2).toMutableList()
        var guard = 0
        while (distractors.size < k - 1 && guard++ < 20) {
            val m = mutate(target, rng)
            if (m != target && m !in distractors) distractors.add(m)
        }
        return (distractors.take(k - 1) + target).shuffled(rng)
    }

    // ---------- Grading ----------
    fun similarity(a: String, b: String): Double {
        val x = stripAccents(cleanWord(a))
        val y = stripAccents(cleanWord(b))
        if (x.isEmpty() && y.isEmpty()) return 1.0
        if (x.isEmpty() || y.isEmpty()) return 0.0
        val dist = levenshtein(x, y)
        return 1.0 - dist.toDouble() / max(x.length, y.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
        return dp[a.length][b.length]
    }

    fun grade(hiddenWords: List<String>, userAnswer: String, lenient: Boolean = true): Grade {
        val userTokens = userAnswer.trim().split(Regex("\\s+")).map { cleanWord(it) }.filter { it.isNotEmpty() }
        val used = mutableSetOf<Int>()
        val threshold = if (lenient) 0.8 else 0.999
        val results = hiddenWords.map { w ->
            val target = cleanWord(w)
            var bestI = -1
            var bestS = 0.0
            userTokens.forEachIndexed { i, ut ->
                if (i !in used) {
                    val s = similarity(ut, target)
                    if (s > bestS) { bestS = s; bestI = i }
                }
            }
            val ok = bestS >= threshold
            if (ok && bestI >= 0) used.add(bestI)
            WordGrade(w, ok, if (bestI >= 0) userTokens[bestI] else null, bestS)
        }
        if (results.isEmpty()) return Grade(true, 1.0, results)
        return Grade(results.all { it.correct }, results.sumOf { it.similarity } / results.size, results)
    }

    fun comboMultiplier(combo: Int): Int = when {
        combo >= 10 -> 10; combo >= 5 -> 5; combo >= 3 -> 3; combo >= 2 -> 2; else -> 1
    }

    fun isPunctuation(token: String) = token in punctuation
}
