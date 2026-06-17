package org.tasks.filters

import com.todoroo.astrid.api.PermaSql
import org.jetbrains.compose.resources.getString
import org.tasks.CommonParcelize
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.Functions
import org.tasks.data.sql.Operator
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.filter_today

@CommonParcelize
data class TodayFilter(
    override val title: String,
    override var filterOverride: String? = null,
) : AstridOrderingFilter() {
    override val sql: String
        get() = QueryTemplate()
            .where(
                Criterion.and(
                    Task.DELETION_DATE.lte(0),
                    Task.HIDE_UNTIL.lte(Functions.now()),
                    Task.DUE_DATE.gt(0),
                    Task.DUE_DATE.lte(PermaSql.VALUE_EOD),
                    completionCriterion()
                )
            )
            .toString()

    /**
     * Builds the completion filter for the Today view:
     * - Uncompleted tasks always show
     * - Completed tasks only show if they were completed today OR due today
     *
     * Uses NOT(completed > 0) instead of completed <= 0 so the global
     * QueryUtils.showCompleted() regex (tasks\.completed<?=0) doesn't
     * match and strip this clause, which would break the OR logic.
     */
    private fun completionCriterion(): Criterion = object : Criterion(Operator.eq) {
        override fun populate() =
            "(" +
            "NOT (${Task.COMPLETION_DATE} > 0) " +
            "OR (${Task.COMPLETION_DATE} > 0 AND ${Task.COMPLETION_DATE} >= ${PermaSql.VALUE_SOD} AND ${Task.COMPLETION_DATE} <= ${PermaSql.VALUE_EOD}) " +
            "OR (${Task.COMPLETION_DATE} > 0 AND ${Task.DUE_DATE} >= ${PermaSql.VALUE_SOD})" +
            ")"
    }

    override val icon: String
        get() = TasksIcons.TODAY

    override val valuesForNewTasks: String
        get() = mapToSerializedString(mapOf(Task.DUE_DATE.name to PermaSql.VALUE_NOON))

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is TodayFilter
    }

    companion object {
        suspend fun create() = TodayFilter(getString(Res.string.filter_today))
    }
}