package com.xucong.wordmemory.data

/**
 * 内置示例词库。
 *
 * 首次启动且数据库为空时由 Repository 写入，保证 App 装完就能看到内容、
 * 也方便实验课现场演示（不依赖任何网络或外部文件）。
 */
object SeedData {

    data class SeedWord(
        val term: String,
        val phonetic: String,
        val meaning: String,
        val example: String,
    )

    data class SeedDeck(
        val name: String,
        val description: String,
        val words: List<SeedWord>,
    )

    val decks: List<SeedDeck> = listOf(
        SeedDeck(
            name = "B4 结课词表",
            description = "课程设计用的示例词库，含 12 个常见词",
            words = listOf(
                SeedWord("abandon", "/əˈbændən/", "v. 放弃；抛弃", "He abandoned the plan after the first failure."),
                SeedWord("benefit", "/ˈbenɪfɪt/", "n. 好处；v. 使受益", "Regular review benefits long-term memory."),
                SeedWord("crucial", "/ˈkruːʃl/", "adj. 至关重要的", "Spaced repetition is crucial for vocabulary retention."),
                SeedWord("degrade", "/dɪˈɡreɪd/", "v. 降低；使退化", "Sleep deprivation degrades attention."),
                SeedWord("efficient", "/ɪˈfɪʃnt/", "adj. 高效的", "An efficient review queue only shows due words."),
                SeedWord("fatigue", "/fəˈtiːɡ/", "n. 疲劳", "Study fatigue builds up without breaks."),
                SeedWord("generate", "/ˈdʒenəreɪt/", "v. 产生；生成", "The app generates a daily review list."),
                SeedWord("highlight", "/ˈhaɪlaɪt/", "v. 强调；n. 亮点", "Highlight the words you keep forgetting."),
                SeedWord("interval", "/ˈɪntəvl/", "n. 间隔", "The interval grows after each correct answer."),
                SeedWord("justify", "/ˈdʒʌstɪfaɪ/", "v. 证明…正当", "Nothing justifies skipping three days of review."),
                SeedWord("keen", "/kiːn/", "adj. 热衷的；敏锐的", "She is keen on learning new words every day."),
                SeedWord("legacy", "/ˈleɡəsi/", "n. 遗产；遗留物", "The legacy database schema is still supported."),
            ),
        ),
        SeedDeck(
            name = "四级高频",
            description = "四级考试高频词，含 10 个",
            words = listOf(
                SeedWord("accommodate", "/əˈkɒmədeɪt/", "v. 容纳；适应", "The library can accommodate 200 readers."),
                SeedWord("consequence", "/ˈkɒnsɪkwəns/", "n. 结果；后果", "Every choice has its consequence."),
                SeedWord("distinguish", "/dɪˈstɪŋɡwɪʃ/", "v. 区分；辨别", "Can you distinguish these two similar words?"),
                SeedWord("enormous", "/ɪˈnɔːməs/", "adj. 巨大的", "The dataset is enormous compared with last year."),
                SeedWord("fundamental", "/ˌfʌndəˈmentl/", "adj. 基础的；根本的", "Data persistence is fundamental to this app."),
                SeedWord("guarantee", "/ˌɡærənˈtiː/", "v./n. 保证", "No method guarantees instant memorization."),
                SeedWord("inevitable", "/ɪnˈevɪtəbl/", "adj. 不可避免的", "Some forgetting is inevitable without review."),
                SeedWord("motivate", "/ˈməʊtɪveɪt/", "v. 激励；促使", "Small daily goals motivate learners."),
                SeedWord("phenomenon", "/fəˈnɒmɪnən/", "n. 现象", "The spacing effect is a well-known phenomenon."),
                SeedWord("retain", "/rɪˈteɪn/", "v. 保持；记住", "Active recall helps you retain more words."),
            ),
        ),
    )
}
