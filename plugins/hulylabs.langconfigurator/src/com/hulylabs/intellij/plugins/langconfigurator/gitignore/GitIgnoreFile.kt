// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.gitignore

import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedReader
import java.io.InputStreamReader

data class GitIgnorePatternPart(val text: String?, val special: String?) {
  companion object {
    val QUESTION = GitIgnorePatternPart(text = null, special = "?")
    val STAR = GitIgnorePatternPart(text = null, special = "*")
    val DOUBLESTAR = GitIgnorePatternPart(text = null, special = "**")
  }

  fun isFileNamePart(): Boolean {
    return this != DOUBLESTAR && !(special?.startsWith('[') ?: false)
  }

  fun asFileNamePart(): String? {
    if (!isFileNamePart()) return null
    if (text != null) return text.replace("*", "\\*").replace("?", "\\?")
    return special
  }
}

data class GitIgnorePattern(val parts: List<GitIgnorePatternPart>, val negated: Boolean, val isRooted: Boolean, val mustBeDir: Boolean) {
  fun isFileNamePattern(): Boolean {
    if (negated || isRooted || mustBeDir) return false
    return parts.all { it.isFileNamePart() }
  }

  fun isPath(): Boolean {
    return isRooted && parts.all { it.special == null }
  }

  fun asPath(): String? {
    if (!isPath()) return null
    val builder = StringBuilder()
    for (part in parts) {
      builder.append(part.text)
    }
    if (builder.endsWith('/')) {
      builder.deleteCharAt(builder.length - 1)
    }
    return builder.toString()
  }

  fun asFileNamePattern(): String? {
    if (!isFileNamePattern()) return null
    return parts.map { it.asFileNamePart() }.joinToString("")
  }

  fun asRegex(): Regex {
    val builder = StringBuilder()
    if (isRooted) {
      builder.append("^")
    }
    for ((idx, part) in parts.withIndex()) {
      if (part.text != null) {
        builder.append(Regex.escape(part.text))
      }
      else if (part == GitIgnorePatternPart.DOUBLESTAR) {
        builder.append(".*?")

      }
      else if (part == GitIgnorePatternPart.STAR) {
        builder.append("[^/]*?")
      }
      else if (part == GitIgnorePatternPart.QUESTION) {
        builder.append("[^/]")
      }
      else {
        builder.append(part.special)
      }
    }
    builder.append("$")
    return Regex(builder.toString())
  }

  companion object {

    private fun addAsteriskSequence(parts: MutableList<GitIgnorePatternPart>, count: Int) {
      if (count == 2) {
        parts.add(GitIgnorePatternPart.DOUBLESTAR)
      }
      else { // Other asterisk sequences work as regular asterisk
        parts.add(GitIgnorePatternPart.STAR)
      }
    }

    private fun addText(parts: MutableList<GitIgnorePatternPart>, partBuilder: java.lang.StringBuilder) {
      if (partBuilder.isNotEmpty()) {
        parts.add(GitIgnorePatternPart(partBuilder.toString(), null))
        partBuilder.clear()
      }
    }

    private fun parseCharacterClass(line: CharSequence, startIdx: Int): Pair<Int, GitIgnorePatternPart>? {
      val builder = StringBuilder("[")
      var currentIdx = startIdx + 1
      val firstChar = line.getOrNull(currentIdx)
      if (firstChar == '!' || firstChar == '^') {
        builder.append("^")
        currentIdx++
      }
      var prevChar: Char? = null
      while (currentIdx < line.length && line.getOrNull(currentIdx) != ']') {
        val currentChar = line[currentIdx]
        if (currentChar == '\\') {
          if (++currentIdx >= line.length) {
            return null
          }
          builder.append(Regex.escape(String(charArrayOf(line[currentIdx]))))
        }
        else if (currentChar == '-' && prevChar != null && currentIdx + 1 < line.length && line[currentIdx + 1] != ']') {
          var nextChar = line[++currentIdx]
          if (nextChar == '\\') {
            if (currentIdx + 1 < line.length) {
              nextChar = line[++currentIdx]
            }
            else {
              return null
            }
          }
          builder.append(Regex.escape(String(charArrayOf(prevChar))))
          builder.append('-')
          builder.append(Regex.escape(String(charArrayOf(nextChar))))
          prevChar = null
          currentIdx++
          continue
        }
        else if (currentChar == '[' && line.getOrNull(currentIdx + 1) == ':') {
          var namedEndIdx = currentIdx + 2
          while (namedEndIdx < line.length && line.getOrNull(namedEndIdx) != ']') {
            namedEndIdx++
          }
          if (namedEndIdx == line.length) {
            return null
          }
          if ((namedEndIdx - 1) < (currentIdx + 2) || line[namedEndIdx - 1] != ':') {
            builder.append("\\[")
            prevChar = '['
            currentIdx++
            continue
          }
          val namedClass = when (line.subSequence(currentIdx + 2, namedEndIdx - 1)) {
            "alnun" -> "\\p{Alnum}"
            "alpha" -> "\\p{Alpha}"
            "blank" -> "\\p{Blank}"
            "cntrl" -> "\\p{Cntrl}"
            "digit" -> "\\p{Digit}"
            "graph" -> "\\p{Graph}"
            "lower" -> "\\p{Lower}"
            "print" -> "\\p{Print}"
            "punct" -> "\\p{Punct}"
            "space" -> "\\p{Space}"
            "upper" -> "\\p{Upper}"
            "xdigit" -> "\\p{XDigit}"
            else -> {
              return null
            }
          }
          builder.append(namedClass)
          prevChar = null
          currentIdx = namedEndIdx
          continue
        }
        else {
          builder.append(Regex.escape(String(charArrayOf(line[currentIdx]))))
        }

        prevChar = currentChar
        currentIdx++
      }
      if (currentIdx < line.length) {
        builder.append("&&[^/]]")
        return currentIdx to GitIgnorePatternPart(text = null, special = builder.toString())
      }
      else {
        return null
      }

    }

    fun parse(pLine: CharSequence): GitIgnorePattern? {
      var patternIsNegated = false
      var patternMustBeDir = false
      var patternIsRooted = false

      var escaped = false
      var inAsteriskSequence = false
      val line = pLine.trim()
      val parts = mutableListOf<GitIgnorePatternPart>()
      val partBuilder = StringBuilder()
      var idx = 0
      while (idx < line.length) {
        val c = line[idx]
        if (c == '!' && idx == 0) {
          patternIsNegated = true
          idx++
          continue
        }
        if (inAsteriskSequence && c != '*') {
          addAsteriskSequence(parts, partBuilder.length)
          inAsteriskSequence = false
          partBuilder.clear()
        }
        if (c == '\\' && !escaped) {
          escaped = true
          idx++
          continue
        }

        if (c == '?' && !escaped) {
          addText(parts, partBuilder)
          parts.add(GitIgnorePatternPart.QUESTION)
          idx++
          continue
        }
        if (c == '*' && !escaped) {
          if (!inAsteriskSequence) {
            addText(parts, partBuilder)
          }
          inAsteriskSequence = true
        }
        if (c == '/') {
          if (idx == line.length - 1) {
            patternMustBeDir = true
            break
          }
          else {
            patternIsRooted = true
          }
        }
        if (c == '[' && !escaped) {
          val parsedClass = parseCharacterClass(line, idx)
          if (parsedClass != null) {
            val (endIdx, part) = parsedClass
            idx = endIdx
            parts.add(part)
            continue
          }
        }
        escaped = false
        partBuilder.append(c)
        idx++
      }
      if (inAsteriskSequence) {
        addAsteriskSequence(parts, partBuilder.length)
      }
      else {
        addText(parts, partBuilder)
      }
      return GitIgnorePattern(parts, patternIsNegated, patternIsRooted, patternMustBeDir)
    }
  }
}

data class GitIgnoreFile(val modificationStamp: Long = -1, val paths: List<String>, val simplePatterns: List<String>, val regexs: List<Pair<GitIgnorePattern, Regex>>) {
  fun matchRegexs(relativePath: String, isDirectory: Boolean): Boolean {
    for (i in regexs.indices.reversed()) {
      val (pattern, regex) = regexs[i]
      if (pattern.mustBeDir && !isDirectory) {
        continue
      }
      val matches = regex.matches(relativePath)
      if (matches) {
        return !pattern.negated
      }
    }
    return false
  }

  companion object {
    private fun trimTrailingWhitespace(line: CharSequence): CharSequence {
      var lastSpaceIdx: Int? = null
      for ((idx, c) in line.withIndex()) {
        if (c == ' ') {
          if (lastSpaceIdx == null) {
            lastSpaceIdx = idx
          }
          continue
        }
        if (c == '\\') {
          if (idx == line.length - 1) {
            return line
          }
        }
        lastSpaceIdx = null
      }
      if (lastSpaceIdx != null) {
        return line.subSequence(0, lastSpaceIdx)
      }
      else {
        return line
      }
    }

    fun parse(file: VirtualFile): GitIgnoreFile {
      val bufferedReader = BufferedReader(InputStreamReader(file.inputStream))
      val patterns = mutableListOf<GitIgnorePattern>()
      var hasNegatedPattern = false
      for (line in bufferedReader.lines()) {
        if (line.startsWith("#")) {
          continue
        }
        val trimmedLine = trimTrailingWhitespace(line)
        if (trimmedLine.isEmpty()) {
          continue
        }
        val pattern = GitIgnorePattern.parse(trimmedLine) ?: continue

        hasNegatedPattern = hasNegatedPattern || pattern.negated
        patterns.add(pattern)
      } // If there is a negated pattern, we need to check all patterns sequentially
      if (hasNegatedPattern) {
        val regexs = patterns.map { it to it.asRegex() }
        return GitIgnoreFile(file.timeStamp, listOf(), listOf(), regexs)
      }
      val paths = mutableListOf<String>()
      val fileNamePatterns = mutableListOf<String>()
      val regexPatterns = mutableListOf<Pair<GitIgnorePattern, Regex>>()
      for (pattern in patterns) {
        if (pattern.isPath()) {
          val path = pattern.asPath()!!
          paths.add(path)
        }
        else if (pattern.isFileNamePattern()) {
          fileNamePatterns.add(pattern.asFileNamePattern()!!)
        }
        else {
          regexPatterns.add(pattern to pattern.asRegex())
        }
      }
      return GitIgnoreFile(file.timeStamp, paths, fileNamePatterns, regexPatterns)
    }
  }
}