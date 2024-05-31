package io.arusland.devzen.parser

/**
 * @author Ruslan Absalyamov
 * @since 1.0
 */

data class Episode(val id: String,
                   val title: String,
                   val url: String,
                   val date: String,
                   val links: List<Link>,
                   val members: List<Link>,
                   val description: String,
                   val gitterLink: Link?,
                   val thumbImageLink: String?,
                   val downloadLink: String?)


data class Link(val url: String, val title: String)
