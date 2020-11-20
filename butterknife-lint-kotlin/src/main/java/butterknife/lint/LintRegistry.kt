package butterknife.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.google.common.collect.ImmutableList

/**
 * Contains references to all custom lint checks for butterknife.
 */
class LintRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = ImmutableList.of(InvalidR2UsageDetector.ISSUE)

    override val api: Int
        get() = CURRENT_API
}