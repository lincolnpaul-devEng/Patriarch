package com.campus.network.ui.onboarding

data class LocalizedOption(
    val id: String,
    val labelEn: String,
    val labelSw: String
) {
    fun label(lang: String): String = if (lang == "sw") labelSw else labelEn
}

data class OnboardingSurveyStrings(
    val langCode: String,
    val continueBtn: String,
    val skipTop: String,
    val skipBtn: String,
    // Step A: Language
    val selectLanguageTitle: String,
    val selectLanguageSubtitle: String,
    // Step B: Country
    val selectCountryTitle: String,
    val selectCountrySubtitle: String,
    val searchCountryPlaceholder: String,
    val featuredCountriesHeader: String,
    val allCountriesHeader: String,
    // Step C: Region / County / State
    val selectRegionTitleFormat: String,
    val selectRegionSubtitleFormat: String,
    val searchRegionPlaceholderFormat: String,
    // Step D: University
    val selectUniversityTitle: String,
    val selectUniversitySubtitleFormat: String,
    val searchUniversityPlaceholder: String,
    val featuredCampusesHeader: String,
    val allCampusesHeader: String,
    val unlistedUniversityOption: String,
    val customUniversityDialogTitle: String,
    val customUniversityInputPlaceholder: String,
    val customUniversityConfirmBtn: String,
    val verifiedEmailBadge: String,
    // Step E: Welcome
    val welcomeTitle: String,
    val welcomeBody: String,
    // Step F: 1 / 4 Academic Profile
    val q1Prompt: String,
    val q1FacultySectionLabel: String,
    val q1YearSectionLabel: String,
    val q1CalloutBubble: String,
    // Step G: 2 / 4 Primary Intent
    val q2Prompt: String,
    val q2CalloutBubble: String,
    // Step H: 3 / 4 Category Interests
    val q3Prompt: String,
    // Step I: 4 / 4 Marketplace Priorities
    val q4Prompt: String,
    // Step J: Push Notifications
    val pushPrompt: String,
    val pushBody: String,
    // Step K: Personalized News
    val personalizedTitle: String,
    val personalizedBody: String
)

object OnboardingSurveyCatalog {
    fun stringsFor(langCode: String): OnboardingSurveyStrings {
        return if (langCode == "sw") {
            OnboardingSurveyStrings(
                langCode = "sw",
                continueBtn = "ENDELEA",
                skipTop = "Ruka",
                skipBtn = "RUKA",
                selectLanguageTitle = "Chagua lugha yako.",
                selectLanguageSubtitle = "Unaweza kubadilisha lugha hii wakati wowote kwenye mipangilio ya wasifu wako.",
                selectCountryTitle = "Chagua nchi yako.",
                selectCountrySubtitle = "Tutaonyesha vyuo vikuu, sarafu na soko la chuo kulingana na nchi yako.",
                searchCountryPlaceholder = "Tafuta nchi...",
                featuredCountriesHeader = "NCHI ZINAZOONGOZA",
                allCountriesHeader = "NCHI ZOTE",
                selectRegionTitleFormat = "Chagua %s yako.",
                selectRegionSubtitleFormat = "Hii inatusaidia kupendekeza vyuo na maeneo salama ya kubadilishana bidhaa karibu nawe.",
                searchRegionPlaceholderFormat = "Tafuta %s...",
                selectUniversityTitle = "Chuo chako kikuu ni kipi?",
                selectUniversitySubtitleFormat = "Inaonyesha vyuo vya %s. Chagua chuo chako au ongeza chuo chako hapa chini.",
                searchUniversityPlaceholder = "Tafuta chuo kikuu au kikoa...",
                featuredCampusesHeader = "VYUO VIKUU MAARUFU",
                allCampusesHeader = "ORODHA YA VYUO VYOTE",
                unlistedUniversityOption = "Chuo changu hakipo kwenye orodha (Ongeza chuo changu)",
                customUniversityDialogTitle = "Andika jina la chuo chako",
                customUniversityInputPlaceholder = "Mfano: Chuo Kikuu cha Teknolojia",
                customUniversityConfirmBtn = "HIFADHI CHUO",
                verifiedEmailBadge = "Imethibitishwa kwa Barua Pepe ya Chuo",
                welcomeTitle = "Karibu Campus Network.",
                welcomeBody = "Tungependa kukufahamu zaidi!\nJibu maswali machache ili tukupendekezee\nbidhaa, huduma na vikundi vinavyokufaa.",
                q1Prompt = "Unasoma kitivo gani na uko mwaka wa ngapi?",
                q1FacultySectionLabel = "CHAGUA KITIVO / SHULE YAKO",
                q1YearSectionLabel = "MWAKA WA MASOMO",
                q1CalloutBubble = "Tutakuunganisha na wanafunzi wa kozi yako na vikundi vya masomo vya mwaka wako!",
                q2Prompt = "Ni nini kilichokuleta hapa? (Unaweza kuchagua zaidi ya moja)",
                q2CalloutBubble = "Tutakutumia mapendekezo na taarifa za chuo kulingana na uchaguzi wako!",
                q3Prompt = "Unavutiwa na kategoria zipi? (Unaweza kuchagua zaidi ya moja)",
                q4Prompt = "Ni mambo gani muhimu kwako unapotumia Soko la Chuo? (Unaweza kuchagua zaidi ya moja)",
                pushPrompt = "Je, ungependa kupokea taarifa za ofa mpya za chuo, mikutano ya ghafla na ujumbe wa SafeTrade?",
                pushBody = "Washa arifa (Push Notifications) kwenye simu yako ili usipitwe na mikutano ya masomo, punguzo la vitabu na uthibitisho wa malipo!",
                personalizedTitle = "Pokea habari na mapendekezo yanayokufaa.",
                personalizedBody = "Kwa kubonyeza endelea, unakubali kupokea mapendekezo yaliyoandaliwa mahususi kwako kulingana na chuo chako, masomo yako na vitu unavyovipenda."
            )
        } else {
            OnboardingSurveyStrings(
                langCode = "en",
                continueBtn = "CONTINUE",
                skipTop = "Skip",
                skipBtn = "SKIP",
                selectLanguageTitle = "Choose your preferred language.",
                selectLanguageSubtitle = "You can change your language anytime in your profile settings.",
                selectCountryTitle = "Where is your campus located?",
                selectCountrySubtitle = "We tailor your campus directory, local currency, and marketplace listings to your country.",
                searchCountryPlaceholder = "Search country...",
                featuredCountriesHeader = "FEATURED COUNTRIES",
                allCountriesHeader = "ALL COUNTRIES (A-Z)",
                selectRegionTitleFormat = "Select your %s.",
                selectRegionSubtitleFormat = "Helps us highlight nearby campuses, study jams, and SafeTrade pickup zones.",
                searchRegionPlaceholderFormat = "Search %s...",
                selectUniversityTitle = "Which university do you attend?",
                selectUniversitySubtitleFormat = "Showing institutions in %s. Pick yours or add it directly below.",
                searchUniversityPlaceholder = "Search university name or domain...",
                featuredCampusesHeader = "TOP & FEATURED UNIVERSITIES",
                allCampusesHeader = "ALL UNIVERSITIES",
                unlistedUniversityOption = "My university is not listed (Enter my university name)",
                customUniversityDialogTitle = "Enter your university or college",
                customUniversityInputPlaceholder = "e.g. Rift Valley Technical Training Institute",
                customUniversityConfirmBtn = "USE THIS UNIVERSITY",
                verifiedEmailBadge = "Auto-matched via your university email",
                welcomeTitle = "Welcome to Campus Network.",
                welcomeBody = "Tell us about yourself!\nTake a quick survey so we can recommend\nthe best items, channels, and services for you.",
                q1Prompt = "Which faculty are you in and what is your year of study?",
                q1FacultySectionLabel = "FACULTY / SCHOOL",
                q1YearSectionLabel = "YEAR OF STUDY",
                q1CalloutBubble = "We'll pin your faculty servers and connect you with course-mates in your year!",
                q2Prompt = "What brings you here? (You can select multiple options)",
                q2CalloutBubble = "We'll send you recommendations and information based on your choices!",
                q3Prompt = "Which categories are you interested in? (You can select multiple options)",
                q4Prompt = "What are the important aspects when purchasing on Campus Marketplace? (You can select multiple options)",
                pushPrompt = "Would you like to receive the latest campus deals, flash-meets, and SafeTrade updates?",
                pushBody = "Please enable Push Notifications in your device settings to receive exclusive campus deals, study jam alerts, and escrow notifications!",
                personalizedTitle = "Receive personalized news and recommendations.",
                personalizedBody = "By clicking continue, you consent to receive tailored recommendations based on your campus profile, academic interests, and marketplace activity."
            )
        }
    }

    val faculties = listOf(
        LocalizedOption("Computing & Informatics", "Computing, IT & Data Science", "Kompyuta, IT na Sayansi ya Data"),
        LocalizedOption("Engineering & Architecture", "Engineering & Built Environment", "Uhandisi na Usanifu Majengo"),
        LocalizedOption("Business & Economics", "Business, Commerce & Economics", "Biashara, Uchumi na Uhasibu"),
        LocalizedOption("Medicine & Health Sciences", "Medicine, Nursing & Pharmacy", "Tiba, Uuguzi na Famasia"),
        LocalizedOption("Law & Governance", "Law & Legal Studies", "Sheria na Utawala"),
        LocalizedOption("Humanities & Social Sciences", "Arts, Media & Social Sciences", "Sanaa, Habari na Sayansi ya Jamii"),
        LocalizedOption("Pure & Applied Sciences", "Biological & Physical Sciences", "Sayansi Asilia na Hisabati"),
        LocalizedOption("Education & Agriculture", "Education, Agriculture & Environment", "Elimu, Kilimo na Mazingira")
    )

    val yearsOfStudy = listOf(
        LocalizedOption("Year 1", "Year 1 (Freshman)", "Mwaka wa 1"),
        LocalizedOption("Year 2", "Year 2 (Sophomore)", "Mwaka wa 2"),
        LocalizedOption("Year 3", "Year 3 (Junior)", "Mwaka wa 3"),
        LocalizedOption("Year 4", "Year 4 (Senior)", "Mwaka wa 4"),
        LocalizedOption("Year 5+", "Year 5+ / Clinical", "Mwaka wa 5+"),
        LocalizedOption("Postgraduate", "Masters / PhD", "Shahada ya Uzamili / PhD"),
        LocalizedOption("Recent Graduate", "Recent Graduate / Alumni", "Mhitimu wa Hivi Karibuni")
    )

    val intents = listOf(
        LocalizedOption(
            "buy_sell_marketplace",
            "Looking to buy or sell textbooks, electronics & dorm gear",
            "Ninataka kununua au kuuza vitabu, vifaa vya kielektroniki na vya hosteli"
        ),
        LocalizedOption(
            "academic_channels",
            "Want to join course channels, study groups & past-paper vaults",
            "Ninataka kujiunga na vikundi vya masomo na kupata mitihani iliyopita"
        ),
        LocalizedOption(
            "micro_gigs",
            "Interested in earning from campus micro-gigs & peer tutoring",
            "Ninavutiwa na kazi fupi za chuo (micro-gigs) na kufundisha wenzangu"
        ),
        LocalizedOption(
            "flash_meets",
            "Want to discover live Flash-Meets, study jams & campus events",
            "Ninataka kugundua mikutano ya ghafla ya masomo na matukio ya chuoni"
        )
    )

    // Variable-width rectangular chips matching Reference Image 6
    val categories = listOf(
        LocalizedOption("Textbooks", "Textbooks", "Vitabu vya Kiada"),
        LocalizedOption("Electronics", "Laptops & Electronics", "Kompyuta na Elektroniki"),
        LocalizedOption("Calculators & Lab Gear", "Calculators & Lab Gear", "Vikokotoo na Vifaa vya Maabara"),
        LocalizedOption("Dorm & Bedding", "Dorm & Bedding", "Vifaa vya Hosteli na Malazi"),
        LocalizedOption("Campus Apparel", "Hoodies & Campus Wear", "Sweta na Mavazi ya Chuo"),
        LocalizedOption("Footwear", "Sneakers & Footwear", "Viatu na Raba"),
        LocalizedOption("Backpacks & Bags", "Backpacks & Bags", "Mabegi ya Mgongoni"),
        LocalizedOption("Audio & Headphones", "Headphones & Audio", "vipokea sauti na Spika"),
        LocalizedOption("Peer Tutoring", "Peer Tutoring", "Mafunzo kwa Wanafunzi"),
        LocalizedOption("UI/UX & Coding Gigs", "Coding & Design Gigs", "Kazi za Programu na Ubunifu"),
        LocalizedOption("Study Snacks", "Coffee & Study Snacks", "Kahawa na Vitafunwa"),
        LocalizedOption("Sports & Fitness", "Sports & Gym Gear", "Vifaa vya Michezo na Gym"),
        LocalizedOption("Stationery", "Notes & Stationery", "Shajara na Vifaa vya Kuandikia"),
        LocalizedOption("Musical Instruments", "Musical Instruments", "Ala za Muziki"),
        LocalizedOption("Bikes & Commute", "Bicycles & Scooters", "Baiskeli na Usafiri"),
        LocalizedOption("Event Tickets", "Campus Event Tickets", "Tiketi za Matukio ya Chuo"),
        LocalizedOption("Internships", "Internships & Careers", "Nafasi za Mafunzo kwa Vitendo")
    )

    // Full-width square cards matching Reference Image 7
    val priorities = listOf(
        LocalizedOption(
            "safe_pickup_escrow",
            "Safe pickup spot & SafeTrade escrow",
            "Eneo salama la kukutana na malipo ya SafeTrade escrow"
        ),
        LocalizedOption(
            "reasonable_price",
            "Reasonable price",
            "Bei nafuu kwa mwanafunzi"
        ),
        LocalizedOption(
            "verified_student_seller",
            "Verified student seller & trust score",
            "Muuzaji mwanafunzi aliyethibitishwa na mwenye uaminifu"
        ),
        LocalizedOption(
            "good_quality_condition",
            "Good quality & clean condition",
            "Ubora mzuri na hali safi ya bidhaa"
        ),
        LocalizedOption(
            "close_walking_distance",
            "Short walking distance on campus",
            "Umbali mfupi wa kutembea chuoni"
        ),
        LocalizedOption(
            "fast_replies",
            "Fast replies & instant availability",
            "Majibu ya haraka na kupatikana mara moja"
        )
    )
}
