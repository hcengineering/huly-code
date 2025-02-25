// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.github.api.data.pullrequest

import com.intellij.collaboration.api.dto.GraphQLFragment
import org.jetbrains.plugins.github.api.data.GHNode

@GraphQLFragment("/graphql/fragment/pullRequestPendingReview.graphql")
class GHPullRequestPendingReviewDTO(
  id: String,
  val state: GHPullRequestReviewState,
  val comments: CommentCount,
) : GHNode(id) {
  data class CommentCount(val totalCount: Int)
}