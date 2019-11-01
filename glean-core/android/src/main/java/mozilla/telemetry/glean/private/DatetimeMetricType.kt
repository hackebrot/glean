/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.telemetry.glean.private

import androidx.annotation.VisibleForTesting
import com.sun.jna.StringArray
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit as AndroidTimeUnit
import mozilla.telemetry.glean.Dispatchers
import mozilla.telemetry.glean.Glean
import mozilla.telemetry.glean.rust.LibGleanFFI
import mozilla.telemetry.glean.rust.getAndConsumeRustString
import mozilla.telemetry.glean.rust.toBoolean
import mozilla.telemetry.glean.rust.toByte
import mozilla.telemetry.glean.testing.ErrorType
import mozilla.telemetry.glean.utils.parseISOTimeString

/**
 * This implements the developer facing API for recording datetime metrics.
 *
 * Instances of this class type are automatically generated by the parsers at build time,
 * allowing developers to record values that were previously registered in the metrics.yaml file.
 */
class DatetimeMetricType internal constructor(
    private var handle: Long,
    private var disabled: Boolean,
    private val sendInPings: List<String>
) {
    /**
     * The public constructor used by automatically generated metrics.
     */
    constructor(
        disabled: Boolean,
        category: String,
        lifetime: Lifetime,
        name: String,
        sendInPings: List<String>,
        timeUnit: TimeUnit = TimeUnit.Minute
    ) : this(handle = 0, disabled = disabled, sendInPings = sendInPings) {
        val ffiPingsList = StringArray(sendInPings.toTypedArray(), "utf-8")
        this.handle = LibGleanFFI.INSTANCE.glean_new_datetime_metric(
            category = category,
            name = name,
            send_in_pings = ffiPingsList,
            send_in_pings_len = sendInPings.size,
            lifetime = lifetime.ordinal,
            disabled = disabled.toByte(),
            time_unit = timeUnit.ordinal
        )
    }

    /**
     * Destroy this metric.
     */
    protected fun finalize() {
        if (this.handle != 0L) {
            LibGleanFFI.INSTANCE.glean_destroy_datetime_metric(this.handle)
        }
    }

    /**
     * Set a datetime value, truncating it to the metric's resolution.
     *
     * @param value The [Date] value to set. If not provided, will record the current time.
     */
    @JvmOverloads
    fun set(value: Date = Date()) {
        val cal = Calendar.getInstance()
        cal.time = value
        set(cal)
    }

    /**
     * Explicitly set a value synchronously.
     *
     * This is only to be used for the glean-ac to glean-core data migration.
     *
     * @param cal The [Calendar] value to set.
     */
    internal fun setSync(cal: Calendar) {
        if (disabled) {
            return
        }

        LibGleanFFI.INSTANCE.glean_datetime_set(
            Glean.handle,
            this@DatetimeMetricType.handle,
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            second = cal.get(Calendar.SECOND),
            nano = AndroidTimeUnit.MILLISECONDS.toNanos(cal.get(Calendar.MILLISECOND).toLong()),
            offset_seconds = AndroidTimeUnit.MILLISECONDS.toSeconds(
                cal.get(Calendar.ZONE_OFFSET).toLong() + cal.get(Calendar.DST_OFFSET)
            ).toInt()
        )
    }

    /**
     * Set a datetime value, truncating it to the metric's resolution.
     *
     * This is provided as an internal-only function for convenience and so that we can
     * test that timezones are passed through correctly.  The normal public interface uses
     * [Date] objects which are always in the local timezone.
     *
     * @param value The [Calendar] value to set. If not provided, will record the current time.
     */
    internal fun set(value: Calendar) {
        if (disabled) {
            return
        }

        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.launch {
            LibGleanFFI.INSTANCE.glean_datetime_set(
                Glean.handle,
                this@DatetimeMetricType.handle,
                year = value.get(Calendar.YEAR),
                month = value.get(Calendar.MONTH) + 1,
                day = value.get(Calendar.DAY_OF_MONTH),
                hour = value.get(Calendar.HOUR_OF_DAY),
                minute = value.get(Calendar.MINUTE),
                second = value.get(Calendar.SECOND),
                nano = AndroidTimeUnit.MILLISECONDS.toNanos(value.get(Calendar.MILLISECOND).toLong()),
                offset_seconds = AndroidTimeUnit.MILLISECONDS.toSeconds(
                        value.get(Calendar.ZONE_OFFSET).toLong() + value.get(Calendar.DST_OFFSET)
                ).toInt()
            )
        }
    }

    /**
     * Tests whether a value is stored for the metric for testing purposes only. This function will
     * attempt to await the last task (if any) writing to the the metric's storage engine before
     * returning a value.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.
     *                 Defaults to the first value in `sendInPings`.
     * @return true if metric value exists, otherwise false
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmOverloads
    fun testHasValue(pingName: String = sendInPings.first()): Boolean {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        return LibGleanFFI
            .INSTANCE.glean_datetime_test_has_value(Glean.handle, this.handle, pingName)
            .toBoolean()
    }

    /**
     * Returns the string representation of the stored value for testing purposes only. This
     * function will attempt to await the last task (if any) writing to the the metric's storage
     * engine before returning a value.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.
     *                 Defaults to the first value in `sendInPings`.
     * @return value of the stored metric
     * @throws [NullPointerException] if no value is stored
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmOverloads
    fun testGetValueAsString(pingName: String = sendInPings.first()): String {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        if (!testHasValue(pingName)) {
            throw NullPointerException()
        }
        val ptr = LibGleanFFI
            .INSTANCE
            .glean_datetime_test_get_value_as_string(Glean.handle, this.handle, pingName)!!
        return ptr.getAndConsumeRustString()
    }

    /**
     * Returns the stored value for testing purposes only. This function will attempt to await the
     * last task (if any) writing to the the metric's storage engine before returning a value.
     *
     * [Date] objects are always in the user's local timezone offset. If you
     * care about checking that the timezone offset was set and sent correctly, use
     * [testGetValueAsString] and inspect the offset.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.
     *                 Defaults to the first value in `sendInPings`.
     * @return value of the stored metric
     * @throws [NullPointerException] if no value is stored
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmOverloads
    fun testGetValue(pingName: String = sendInPings.first()): Date {
        return parseISOTimeString(testGetValueAsString(pingName))!!
    }

    /**
     * Returns the number of errors recorded for the given metric.
     *
     * @param errorType The type of the error recorded.
     * @param pingName represents the name of the ping to retrieve the metric for.
     *                 Defaults to the first value in `sendInPings`.
     * @return the number of errors recorded for the metric.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmOverloads
    fun testGetNumRecordedErrors(errorType: ErrorType, pingName: String = sendInPings.first()): Int {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        return LibGleanFFI.INSTANCE.glean_datetime_test_get_num_recorded_errors(
            Glean.handle, this.handle, errorType.ordinal, pingName
        )
    }
}
