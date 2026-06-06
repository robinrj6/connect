package com.example.connect.model

data class EmojiAction(val emoji: String, val label: String)

val allEmojis = listOf(
    EmojiAction("💋", "Kiss"),
    EmojiAction("🤗", "Hug"),
    EmojiAction("❤️", "Love"),
    EmojiAction("💭", "Thinking of you"),
    EmojiAction("😊", "Happy"),
    EmojiAction("🥰", "Adore"),
    EmojiAction("😢", "Miss you"),
    EmojiAction("🥺", "Longing"),
    EmojiAction("💔", "Heartbreak"),
    EmojiAction("😠", "Angry"),
    EmojiAction("😤", "Frustrated"),
    EmojiAction("🙄", "Eye roll"),
    EmojiAction("😒", "Annoyed"),
    EmojiAction("🤦", "Facepalm"),
    EmojiAction("😜", "Teasing"),
    EmojiAction("👻", "Boo"),
    EmojiAction("🤩", "Wow"),
)
