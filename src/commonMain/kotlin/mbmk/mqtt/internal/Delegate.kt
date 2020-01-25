package mbmk.mqtt.internal

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal fun <T> changeable(initialValue: T, onChange: (newValue: T) -> Unit): ReadWriteProperty<Any?, T> {

    return OnPropertyChanged(initialValue, onChange = onChange)
}

private class OnPropertyChanged<in R, T>(
    private var value: T,
    private val isEqual: (t1: T, t2: T) -> Boolean = { t1, t2 -> t1 == t2 },
    private val onChange: (newValue: T) -> Unit
) : ReadWriteProperty<R, T> {

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        val notify = isEqual(this.value, value).not()
        this.value = value
        if (notify) {
            onChange(value)
        }
    }
}