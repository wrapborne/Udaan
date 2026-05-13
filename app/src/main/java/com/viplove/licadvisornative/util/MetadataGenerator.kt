package com.viplove.licadvisornative.util

import com.viplove.licadvisornative.model.DocumentCategories
import com.viplove.licadvisornative.model.DocumentMetadata

/**
 * OPTIMIZED Utility for generating document metadata from extracted text
 * Includes smart categorization, tag extraction, and bilingual search mapping
 */
class MetadataGenerator {

    companion object {
        // Expanded Hindi stopwords (100+ common words to exclude from tags)
        private val HINDI_STOPWORDS = setOf(
            "का", "के", "की", "को", "से", "में", "पर", "है", "हैं", "था", "थी", "थे",
            "और", "या", "तथा", "एवं", "यह", "वह", "इस", "उस", "ये", "वे",
            "तो", "ही", "भी", "कर", "कि", "अब", "तक", "जो", "हो", "गया", "गयी",
            "कृपया", "श्रीमान", "श्रीमती", "महोदय", "महोदया", "जी", "द्वारा", "हेतु",
            "लिए", "साथ", "बाद", "पहले", "दौरान", "अंदर", "बाहर", "ऊपर", "नीचे",
            "सभी", "कुछ", "बहुत", "अधिक", "कम", "पूरा", "आधा", "एक", "दो", "तीन",
            "कोई", "किसी", "कुछ", "सब", "हर", "प्रत्येक", "अन्य", "दूसरा", "वाला",
            "करना", "होना", "जाना", "आना", "देना", "लेना", "रहना", "चाहिए", "सकता",
            "करें", "करे", "होगा", "होगी", "होंगे", "जाएगा", "आएगा", "रहेगा",
            "महोदय", "सर", "मैडम", "जी", "महोदया", "कृपया", "धन्यवाद", "नमस्कार"
        )
        
        // Expanded English stopwords (100+ common words)
        private val ENGLISH_STOPWORDS = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "be",
            "been", "being", "have", "has", "had", "do", "does", "did", "will",
            "would", "should", "could", "may", "might", "must", "can", "this",
            "that", "these", "those", "i", "you", "he", "she", "it", "we", "they",
            "sir", "madam", "mr", "mrs", "ms", "dear", "kindly", "please", "thank",
            "regards", "sincerely", "yours", "insurance", "policy", "form", "office",
            "use", "only", "fill", "block", "letters", "capital", "date", "signature",
            "name", "address", "phone", "email", "number", "code", "pin", "area",
            "city", "state", "country", "india", "indian", "above", "below", "said",
            "same", "such", "here", "there", "where", "when", "who", "what", "which",
            "how", "all", "any", "some", "few", "many", "more", "most", "other",
            "next", "last", "first", "second", "third", "before", "after", "during"
        )
        
        // Whitelist for critical short-form terms (3-4 letters)
        private val SHORT_TERM_WHITELIST = setOf(
            "gst", "pan", "kyc", "nom", "neft", "rtgs", "ifsc", "lic", "dob", "doc",
            "pwd", "pwb", "ulip", "nav", "aum", "irda", "atm", "otp", "uin", "gsv",
            "ssv", "sum", "age", "term", "mode", "la", "do", "bo"
        )
        
        // Boilerplate phrases to remove from titles
        private val BOILERPLATE_PHRASES = setOf(
            "for office use only", "please fill in block letters", "in capital letters",
            "affix passport size photograph", "attach self-attested copies",
            "life insurance corporation of india", "lic of india", "lic india",
            "instruction", "instructions", "note", "notes", "important"
        )
        
        // Category detection with weighted scoring
        private val CATEGORY_WEIGHTS = mapOf(
            DocumentCategories.POLICY_FORMS to mapOf(
                "proposal" to 5, "प्रस्ताव" to 5,
                "policy" to 3, "पॉलिसी" to 3,
                "premium" to 2, "प्रीमियम" to 2,
                "insurance" to 1, "बीमा" to 1
            ),
            DocumentCategories.CLAIM_FORMS to mapOf(
                "death" to 5, "मृत्यु" to 5,
                "claim" to 4, "दावा" to 4,
                "maturity" to 3, "परिपक्वता" to 3,
                "settlement" to 2, "निपटान" to 2
            ),
            DocumentCategories.REGISTRATION_FORMS to mapOf(
                "registration" to 5, "पंजीकरण" to 5,
                "enrollment" to 4, "नामांकन" to 4,
                "agent" to 3, "एजेंट" to 3,
                "join" to 2, "शामिल" to 2
            ),
            DocumentCategories.SURRENDER_FORMS to mapOf(
                "surrender" to 5, "समर्पण" to 5,
                "discontinue" to 3, "बंद" to 3,
                "terminate" to 2, "समाप्त" to 2,
                "foreclosure" to 2
            ),
            DocumentCategories.LOAN_FORMS to mapOf(
                "loan" to 5, "ऋण" to 5,
                "advance" to 3, "अग्रिम" to 3,
                "borrow" to 2
            ),
            DocumentCategories.ULIP_FORMS to mapOf(
                "ulip" to 5, "यूलिप" to 5,
                "unit" to 3, "यूनिट" to 3,
                "linked" to 3,
                "fund" to 2, "फंड" to 2,
                "nav" to 3,
                "anand" to 4, "lakshya" to 4, "umang" to 4 // LIC plan names
            ),
            "Service Request" to mapOf(
                "service" to 4, "सेवा" to 4,
                "request" to 3, "अनुरोध" to 3,
                "change" to 2, "परिवर्तन" to 2,
                "update" to 2, "अपडेट" to 2,
                "modification" to 2
            ),
            "Nomination/Assignment" to mapOf(
                "nomination" to 5, "नामांकन" to 5,
                "nominee" to 4, "नामित" to 4,
                "assignment" to 4, "असाइनमेंट" to 4,
                "assignee" to 3,
                "beneficiary" to 3
            ),
            DocumentCategories.MEDICAL_FORMS to mapOf(
                "medical" to 5, "चिकित्सा" to 5,
                "health" to 4, "स्वास्थ्य" to 4,
                "doctor" to 3, "डॉक्टर" to 3,
                "report" to 3, "रिपोर्ट" to 3,
                "tpa" to 4, "hospital" to 3, "अस्पताल" to 3,
                "checkup" to 3, "जाँच" to 3,
                "diagnosis" to 4, "treatment" to 3, "इलाज" to 3
            ),
            DocumentCategories.GENERAL_CIRCULARS to mapOf(
                "circular" to 5, "परिपत्र" to 5,
                "notice" to 3, "सूचना" to 3,
                "notification" to 3, "अधिसूचना" to 3
            ),
            DocumentCategories.POLICY_UPDATES to mapOf(
                "update" to 4, "अपडेट" to 4,
                "amendment" to 5, "संशोधन" to 5,
                "revision" to 4, "पुनरीक्षण" to 4,
                "change" to 2, "परिवर्तन" to 2,
                "modify" to 2
            ),
            "Marketing/Announcements" to mapOf(
                "announcement" to 5, "घोषणा" to 5,
                "marketing" to 4,
                "campaign" to 3,
                "promotion" to 3,
                "news" to 2, "समाचार" to 2
            )
        )
        
        // Bilingual keyword mapping (Hindi -> English)
        private val BILINGUAL_MAPPING = mapOf(
            "बीमा" to "insurance",
            "पॉलिसी" to "policy",
            "प्रीमियम" to "premium",
            "दावा" to "claim",
            "मृत्यु" to "death",
            "परिपक्वता" to "maturity",
            "समर्पण" to "surrender",
            "ऋण" to "loan",
            "नामित" to "nominee",
            "प्रस्ताव" to "proposal",
            "एजेंट" to "agent",
            "बोनस" to "bonus",
            "रकम" to "amount",
            "सुम" to "sum",
            "बीमित" to "assured",
            "फॉर्म" to "form",
            "संख्या" to "number",
            "तारीख" to "date",
            "नाम" to "name",
            "पता" to "address"
        )
        
        // Root word stemming map
        private val ROOT_WORD_MAP = mapOf(
            "claimed" to "claim", "claiming" to "claim", "claims" to "claim",
            "insured" to "insure", "insuring" to "insure", "insures" to "insure",
            "policies" to "policy",
            "premiums" to "premium",
            "surrendered" to "surrender", "surrendering" to "surrender",
            "matured" to "maturity", "maturing" to "maturity",
            "nominated" to "nominee", "nominating" to "nominee", "nominates" to "nominee",
            "assigned" to "assignment", "assigning" to "assignment", "assigns" to "assignment"
        )
    }

    /**
     * Generate metadata from extracted text with enhanced algorithms
     */
    fun generateMetadata(
        text: String,
        confidence: Float,
        language: String
    ): DocumentMetadata {
        val warnings = mutableListOf<String>()
        
        // Extract title with enhanced logic
        val title = extractTitleEnhanced(text).also {
            if (it.isBlank()) warnings.add("Could not auto-detect title")
        }
        
        // Detect category with weighted scoring
        val category = detectCategoryWeighted(text).also {
            if (it == DocumentCategories.OTHER) warnings.add("Could not auto-detect category")
        }
        
        // Extract tags with entity recognition
        val tags = extractTagsEnhanced(text, language, title)
        
        return DocumentMetadata(
            suggestedTitle = title,
            detectedCategory = category,
            extractedTags = tags,
            detectedLanguage = language,
            extractedText = text,
            confidence = confidence,
            warnings = warnings
        )
    }

    /**
     * ENHANCED: Extract document title from first 15 lines (up to 1000 chars)
     * with pattern matching and visual hierarchy
     */
    private fun extractTitleEnhanced(text: String): String {
        val lines = text.take(1000) // First 1000 characters
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(15) // First 15 lines
        
        if (lines.isEmpty()) return ""
        
        // Priority 1: Form No. pattern
        val formNoPattern = Regex("""form\s+no\.?\s*(\d+[-/]?\d*)""", RegexOption.IGNORE_CASE)
        lines.forEach { line ->
            formNoPattern.find(line)?.let {
                return sanitizeTitle(line)
            }
        }
        
        // Priority 2: Subject: pattern
        val subjectPattern = Regex("""subject\s*:\s*(.+)""", RegexOption.IGNORE_CASE)
        lines.forEach { line ->
            subjectPattern.find(line)?.let {
                return sanitizeTitle(it.groupValues[1])
            }
        }
        
        // Priority 3: ALL CAPS heading (10-120 chars)
        val allCapsLines = lines
            .filter { it.length in 10..120 }
            .filter { line -> line.all { c -> !c.isLetter() || c.isUpperCase() } }
        
        if (allCapsLines.isNotEmpty()) {
            return sanitizeTitle(allCapsLines.maxByOrNull { it.length } ?: allCapsLines.first())
        }
        
        // Priority 4: Longest line in reasonable range (10-120 chars)
        val titleCandidate = lines
            .filter { it.length in 10..120 }
            .maxByOrNull { it.length }
            ?: lines.firstOrNull()
            ?: ""
        
        return sanitizeTitle(titleCandidate).take(120)
    }

    /**
     * Sanitize title by removing boilerplate phrases
     */
    private fun sanitizeTitle(title: String): String {
        var sanitized = title.trim()
        
        BOILERPLATE_PHRASES.forEach { phrase ->
            sanitized = sanitized.replace(phrase, "", ignoreCase = true)
        }
        
        return sanitized.trim()
    }

    /**
     * ENHANCED: Detect category using weighted scoring model
     */
    private fun detectCategoryWeighted(text: String): String {
        val lowerText = text.lowercase()
        
        val categoryScores = CATEGORY_WEIGHTS.mapValues { (_, keywords) ->
            keywords.entries.sumOf { (keyword, weight) ->
                if (lowerText.contains(keyword.lowercase())) weight else 0
            }
        }
        
        val bestCategory = categoryScores.maxByOrNull { it.value }
        
        return if (bestCategory != null && bestCategory.value >= 3) {
            bestCategory.key
        } else {
            DocumentCategories.OTHER
        }
    }

    /**
     * ENHANCED: Extract tags with entity recognition and weighted frequency
     */
    private fun extractTagsEnhanced(text: String, language: String, title: String): List<String> {
        val tags = mutableSetOf<String>()
        
        // Extract Form IDs (alphanumeric patterns)
        val formIdPattern = Regex("""\b(?:form|फॉर्म)\s*(?:no\.?|संख्या)?\s*([a-z0-9/-]+)\b""", RegexOption.IGNORE_CASE)
        formIdPattern.findAll(text).forEach { match ->
            tags.add(match.groupValues[1].lowercase())
        }
        
        // Extract Section numbers
        val sectionPattern = Regex("""\bsection\s+(\d+[a-z]?)\b""", RegexOption.IGNORE_CASE)
        sectionPattern.findAll(text).forEach { match ->
            tags.add("section-${match.groupValues[1]}")
        }
        
        // Extract LIC Plan names
        val planNames = setOf("anand", "lakshya", "umang", "jeevan", "bima", "arogya", "nivesh")
        text.split("""\s+""".toRegex()).forEach { word ->
            if (planNames.contains(word.lowercase())) {
                tags.add(word.lowercase())
            }
        }
        
        // Word frequency analysis with weighted scoring
        val words = text.split("""[\s\n\r\t,।.।]+""".toRegex())
            .map { it.trim().lowercase() }
            .filter { it.length >= 3 } // Minimum 3 characters
        
        val wordCounts = mutableMapOf<String, Int>()
        
        // Count words with title keywords getting double weight
        val titleWords = title.lowercase().split("""[\s,।.]+""".toRegex()).toSet()
        words.forEach { word ->
            val weight = if (titleWords.contains(word)) 2 else 1
            wordCounts[word] = (wordCounts[word] ?: 0) + weight
        }
        
        // Determine stopwords
        val stopwords = when (language) {
            "hindi" -> HINDI_STOPWORDS
            "english" -> ENGLISH_STOPWORDS
            "both" -> HINDI_STOPWORDS + ENGLISH_STOPWORDS
            else -> ENGLISH_STOPWORDS
        }
        
        // Extract top keywords
        val extractedKeywords = wordCounts
            .filterKeys { word ->
                val lowerWord = word.lowercase()
                // Keep if in whitelist or not in stopwords
                SHORT_TERM_WHITELIST.contains(lowerWord) ||
                (!stopwords.contains(lowerWord) && 
                 lowerWord.length >= 3 &&
                 lowerWord.any { it.isLetter() })
            }
            .toList()
            .sortedByDescending { it.second }
            .take(15)
            .map { it.first }
        
        tags.addAll(extractedKeywords)
        
        return tags.take(10).distinct()
    }

    /**
     * ENHANCED: Generate searchable terms with bilingual mapping and root-word stemming
     */
    fun generateSearchTerms(
        title: String,
        category: String,
        tags: List<String>,
        fullText: String
    ): List<String> {
        val terms = mutableSetOf<String>()
        
        // Add title words
        terms.addAll(title.lowercase().split("""[\s,।.]+""".toRegex()))
        
        // Add category
        terms.add(category.lowercase())
        
        // Add tags
        terms.addAll(tags.map { it.lowercase() })
        
        // Add bilingual mappings
        BILINGUAL_MAPPING.forEach { (hindi, english) ->
            if (fullText.contains(hindi, ignoreCase = true)) {
                terms.add(english)
                terms.add(hindi)
            }
        }
        
        // Add root words for stemming
        ROOT_WORD_MAP.forEach { (variant, root) ->
            if (fullText.contains(variant, ignoreCase = true)) {
                terms.add(root)
            }
        }
        
        // Extract alphanumeric Form IDs regardless of length
        val formIdPattern = Regex("""\b[a-z0-9]+-?[a-z0-9]+\b""", RegexOption.IGNORE_CASE)
        formIdPattern.findAll(fullText).forEach { match ->
            terms.add(match.value.lowercase())
        }
        
        // Add significant words from full text (4+ chars, appears 2+ times)
        val textWords = fullText.split("""[\s\n\r\t,।.]+""".toRegex())
            .map { it.trim().lowercase() }
            .filter { it.length >= 4 } // Changed from 5+ to 4+
            .groupingBy { it }
            .eachCount()
            .filterValues { it >= 2 }
            .keys
        
        terms.addAll(textWords.take(20))
        
        // Clean and return
        return terms
            .filter { it.isNotBlank() && it.length >= 2 } // Minimum 2 chars for search
            .distinct()
            .sorted()
    }
}
