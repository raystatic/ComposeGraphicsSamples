package com.example.graphicspoc.captions.hinglish

/**
 * Spellings that Indian creators actually use for very frequent words, plus English words that
 * speech recognisers tend to write in Devanagari ("वीडियो" -> "video").
 *
 * Phonetic rules alone would produce "nahin", "veediyo", "sabsakraib" – readable but not how
 * anybody types Hinglish. Add to [ENTRIES] freely; keys are normalised on load.
 */
object HinglishLexicon {

    fun lookup(devanagariWord: String): String? = table[devanagariWord]

    private val table: Map<String, String> by lazy {
        ENTRIES.entries.associate { (k, v) -> HinglishTransliterator.normalize(k) to v }
    }

    private val ENTRIES = mapOf(
        // --- Hindi function words & very common words ---
        "है" to "hai", "हैं" to "hain", "हूँ" to "hoon", "हूं" to "hoon", "हो" to "ho",
        "था" to "tha", "थी" to "thi", "थे" to "the", "थीं" to "thi",
        "में" to "mein", "मैं" to "main", "मैंने" to "maine", "मुझे" to "mujhe", "मुझको" to "mujhko",
        "मेरा" to "mera", "मेरी" to "meri", "मेरे" to "mere",
        "तेरा" to "tera", "तेरी" to "teri", "तेरे" to "tere", "तुझे" to "tujhe",
        "नहीं" to "nahi", "नही" to "nahi", "नहि" to "nahi",
        "क्या" to "kya", "क्यों" to "kyun", "क्यूँ" to "kyun", "क्यूं" to "kyun",
        "क्योंकि" to "kyunki", "क्यूंकि" to "kyunki",
        "यह" to "yeh", "ये" to "ye", "वह" to "woh", "वो" to "woh", "वे" to "woh",
        "और" to "aur", "तो" to "toh", "भी" to "bhi", "ही" to "hi",
        "की" to "ki", "के" to "ke", "का" to "ka", "को" to "ko", "से" to "se",
        "पे" to "pe", "पर" to "par", "कि" to "ki", "तक" to "tak",
        "हम" to "hum", "हमें" to "humein", "हमारा" to "hamara", "हमारी" to "hamari", "हमारे" to "hamare",
        "तुम" to "tum", "तुम्हें" to "tumhe", "तुम्हारा" to "tumhara", "तुम्हारी" to "tumhari", "तुम्हारे" to "tumhare",
        "आप" to "aap", "आपको" to "aapko", "आपका" to "aapka", "आपकी" to "aapki", "आपके" to "aapke",
        "उसका" to "uska", "उसकी" to "uski", "उसके" to "uske", "उसे" to "use", "उन्हें" to "unhe",
        "इसका" to "iska", "इसकी" to "iski", "इसके" to "iske", "इसे" to "ise", "इसमें" to "isme", "उसमें" to "usme",
        "कुछ" to "kuch", "बहुत" to "bahut", "ज़्यादा" to "zyada", "ज्यादा" to "zyada",
        "अच्छा" to "accha", "अच्छी" to "acchi", "अच्छे" to "acche",
        "पहले" to "pehle", "पहला" to "pehla", "पहली" to "pehli",
        "इसलिए" to "isliye", "इसलिये" to "isliye", "लिए" to "liye", "लिये" to "liye",
        "कैसे" to "kaise", "कैसा" to "kaisa", "कैसी" to "kaisi",
        "जैसे" to "jaise", "ऐसे" to "aise", "ऐसा" to "aisa", "ऐसी" to "aisi",
        "वैसे" to "waise", "वाला" to "wala", "वाली" to "wali", "वाले" to "wale",
        "यहाँ" to "yahan", "यहां" to "yahan", "वहाँ" to "wahan", "वहां" to "wahan",
        "कहाँ" to "kahan", "कहां" to "kahan", "जहाँ" to "jahan", "जहां" to "jahan",
        "अभी" to "abhi", "सभी" to "sabhi", "कभी" to "kabhi", "तभी" to "tabhi", "जभी" to "jabhi",
        "एक" to "ek", "दो" to "do", "तीन" to "teen", "चार" to "chaar", "पाँच" to "paanch", "पांच" to "paanch",
        "दोस्तों" to "doston", "दोस्तो" to "doston", "यार" to "yaar", "भाई" to "bhai", "जी" to "ji",
        "हाँ" to "haan", "हां" to "haan", "ना" to "na", "न" to "na", "मत" to "mat", "बस" to "bas",
        "माँ" to "maa", "मां" to "maa",
        "ठीक" to "theek", "सच" to "sach", "सही" to "sahi", "गलत" to "galat", "ग़लत" to "galat",
        "ज़रूर" to "zaroor", "जरूर" to "zaroor", "शुरू" to "shuru", "शुक्रिया" to "shukriya",
        "धन्यवाद" to "dhanyavaad", "नमस्ते" to "namaste", "नमस्कार" to "namaskar",
        "आज" to "aaj", "कल" to "kal", "अब" to "ab", "जब" to "jab", "तब" to "tab", "सब" to "sab",
        "लोग" to "log", "लोगों" to "logon", "चीज़" to "cheez", "चीज" to "cheez", "चीज़ें" to "cheezein",
        "पैसे" to "paise", "पैसा" to "paisa", "रुपये" to "rupaye", "रुपए" to "rupaye",
        "गया" to "gaya", "गई" to "gayi", "गयी" to "gayi", "गए" to "gaye", "गये" to "gaye",
        "हुआ" to "hua", "हुई" to "hui", "हुए" to "hue",
        "किया" to "kiya", "दिया" to "diya", "लिया" to "liya", "पिया" to "piya",
        "करें" to "karein", "जाएं" to "jaayein", "जाएँ" to "jaayein",
        "आइए" to "aaiye", "आइये" to "aaiye", "चलिए" to "chaliye", "देखिए" to "dekhiye", "बताइए" to "bataiye",
        "कौन" to "kaun", "कौनसा" to "kaunsa", "कितना" to "kitna", "कितने" to "kitne", "कितनी" to "kitni",
        "मतलब" to "matlab", "बिल्कुल" to "bilkul", "एकदम" to "ekdam", "मस्त" to "mast",
        "प्यार" to "pyaar", "ज़िंदगी" to "zindagi", "जिंदगी" to "zindagi", "दिल" to "dil",
        "वीडियोज़" to "videos",

        // --- English words commonly written in Devanagari by ASR ---
        "वीडियो" to "video", "वीडियोस" to "videos", "चैनल" to "channel",
        "सब्सक्राइब" to "subscribe", "लाइक" to "like", "लाइक्स" to "likes", "शेयर" to "share",
        "कमेंट" to "comment", "कमेंट्स" to "comments", "फॉलो" to "follow", "फ़ॉलो" to "follow",
        "बेल" to "bell", "आइकन" to "icon", "ओके" to "ok", "सॉरी" to "sorry",
        "थैंक" to "thank", "थैंक्स" to "thanks", "थैंक्यू" to "thank you", "यू" to "you",
        "गाइज़" to "guys", "गाइज" to "guys", "गाइस" to "guys", "फ्रेंड्स" to "friends", "फ्रेंड" to "friend",
        "प्रोडक्ट" to "product", "रिव्यू" to "review", "टिप्स" to "tips", "ट्रिक" to "trick", "ट्रिक्स" to "tricks",
        "लिंक" to "link", "बायो" to "bio", "डिस्क्रिप्शन" to "description",
        "इंस्टाग्राम" to "instagram", "यूट्यूब" to "youtube", "रील" to "reel", "रील्स" to "reels",
        "कंटेंट" to "content", "क्रिएटर" to "creator", "क्रिएटर्स" to "creators",
        "फोन" to "phone", "फ़ोन" to "phone", "मोबाइल" to "mobile", "ऐप" to "app", "एप" to "app",
        "डाउनलोड" to "download", "ऑनलाइन" to "online", "ऑफलाइन" to "offline",
        "बिज़नेस" to "business", "बिजनेस" to "business", "मार्केटिंग" to "marketing",
        "सोशल" to "social", "मीडिया" to "media", "टाइम" to "time", "डे" to "day", "लाइफ" to "life",
        "हेल्थ" to "health", "फिटनेस" to "fitness", "वर्कआउट" to "workout", "डाइट" to "diet",
        "रेसिपी" to "recipe", "स्टेप" to "step", "स्टेप्स" to "steps",
        "फर्स्ट" to "first", "सेकंड" to "second", "लास्ट" to "last", "बेस्ट" to "best",
        "प्लीज़" to "please", "प्लीज" to "please", "हेलो" to "hello", "हैलो" to "hello",
        "हाय" to "hi", "बाय" to "bye",
        "एक्चुअली" to "actually", "बेसिकली" to "basically", "लिटरली" to "literally",
        "सीरियसली" to "seriously", "ऑलरेडी" to "already", "वेरी" to "very", "गुड" to "good",
        "बैड" to "bad", "कूल" to "cool", "अमेज़िंग" to "amazing", "अमेजिंग" to "amazing",
        "ऑसम" to "awesome", "स्पेशल" to "special", "न्यू" to "new", "पार्ट" to "part",
        "टॉपिक" to "topic", "पॉइंट" to "point", "प्रॉब्लम" to "problem", "प्रोब्लम" to "problem",
        "सॉल्यूशन" to "solution", "आइडिया" to "idea", "मनी" to "money", "सेविंग" to "saving",
        "सेविंग्स" to "savings", "इन्वेस्टमेंट" to "investment", "स्टॉक" to "stock", "स्टॉक्स" to "stocks",
        "मार्केट" to "market", "जॉब" to "job", "ऑफिस" to "office", "कॉलेज" to "college",
        "स्कूल" to "school", "स्टूडेंट" to "student", "स्टूडेंट्स" to "students",
        "एग्जाम" to "exam", "एग्ज़ाम" to "exam", "क्लास" to "class", "ट्रैवल" to "travel", "ट्रिप" to "trip",
        "होटल" to "hotel", "फूड" to "food", "कैफे" to "cafe", "रेस्टोरेंट" to "restaurant",
        "प्लान" to "plan", "ब्रांड" to "brand", "स्किन" to "skin", "केयर" to "care", "हेयर" to "hair",
        "मेकअप" to "makeup", "फैशन" to "fashion", "स्टाइल" to "style", "लुक" to "look", "ड्रेस" to "dress",
        "शॉपिंग" to "shopping", "सेल" to "sale", "ऑफर" to "offer", "डिस्काउंट" to "discount",
        "कोड" to "code", "फ्री" to "free", "प्राइस" to "price", "कंपनी" to "company",
        "सर" to "sir", "मैडम" to "madam", "टीम" to "team", "गेम" to "game", "मूवी" to "movie",
        "सॉन्ग" to "song", "म्यूजिक" to "music", "म्यूज़िक" to "music", "फैमिली" to "family",
        "ट्रेंड" to "trend", "ट्रेंडिंग" to "trending", "वायरल" to "viral", "पोस्ट" to "post",
        "स्टोरी" to "story", "लाइव" to "live", "सेटअप" to "setup", "कैमरा" to "camera",
        "लैपटॉप" to "laptop", "इंटरनेट" to "internet", "वेबसाइट" to "website", "ऑर्डर" to "order",
        "डिलीवरी" to "delivery", "क्वालिटी" to "quality", "रिजल्ट" to "result", "रिज़ल्ट" to "result",
    )
}
