package org.simbrain.workspace.couplings

import org.simbrain.workspace.*
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import java.lang.reflect.Method
import java.lang.reflect.Type

typealias JConsumer<T> = java.util.function.Consumer<T>

/**
 * A cache of all [Attribute] and [Coupling] objects in the [Workspace]. Used to provide
 * fast indexed access to producers, consumers, couplings, etc., and to filter these quickly.
 */
class CouplingCache(workspace: Workspace) {

    /**
     * A cache of all producers in the current workspace
     */
    val producers = ProducerCache()

    /**
     * A cache of all consumers in the current workspace
     */
    val consumers = ConsumerCache()

    /**
     * A collection of all couplings grouped by [AttributeContainer]s. Neuron1 -> Coupling, Neuron 2->Coupling
     */
    private val couplingsByContainer = HashMap<AttributeContainer, HashSet<Coupling>>()

    /**
     * A collection of all producible and consumable methods that are visible
     */
    val visibleMethods
        get() = producers.visibleMethods + consumers.visibleMethods

    /**
     * A collection of all couplings. This is mainly for serialization.
     */
    val couplings
        get() = couplingsByContainer.values.flatten().toSet().sortedBy { it.id }

    init {
        workspace.events.apply {

            onComponentAdded(JConsumer {

                it.attributeContainers.forEach{ ac ->
                    producers.addContainer(it, ac)
                    consumers.addContainer(it, ac)
                }

                it.events.apply {

                    onAttributeContainerAdded(JConsumer { ac ->
                        producers.addContainer(it, ac)
                        consumers.addContainer(it, ac)
                    })

                    onAttributeContainerRemoved(JConsumer { ac ->
                        producers.removeContainer(it, ac)
                        consumers.removeContainer(it, ac)
                        val couplingsCopy = couplingsByContainer[ac]?.toList()
                        couplingsByContainer.remove(ac)
                        couplingsCopy?.let { it1 ->
                            workspace.couplingManager.events.fireCouplingsRemoved(it1)
                        }
                    })
                }
            })

            onComponentRemoved(JConsumer {
                it.attributeContainers.forEach { ac ->
                    producers.removeContainer(it, ac)
                    consumers.removeContainer(it, ac)
                    couplingsByContainer.remove(ac)
                }
            })

        }

    }

    /**
     * Adds a coupling to the cache.
     */
    fun add(coupling: Coupling) {
        val producerContainer = coupling.producer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }
        val consumerContainer = coupling.consumer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }

        if (producerContainer in couplingsByContainer) {
            couplingsByContainer[producerContainer]!!.add(coupling)
        } else {
            couplingsByContainer[producerContainer] = hashSetOf(coupling)
        }

        if (consumerContainer in couplingsByContainer) {
            couplingsByContainer[consumerContainer]!!.add(coupling)
        } else {
            couplingsByContainer[consumerContainer] = hashSetOf(coupling)
        }
    }

    /**
     * Removes a coupling from the cache
     */
    fun remove(coupling: Coupling) {
        val producerContainer = coupling.producer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }
        val consumerContainer = coupling.consumer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }

        couplingsByContainer[producerContainer]?.remove(coupling)
        couplingsByContainer[consumerContainer]?.remove(coupling)
    }

    /**
     * Sets an [Attribute.method] to being visible, i.e. visible in the GUI.
     */
    fun setVisible(method: Method, visible: Boolean) {
        val cache = when {
            method.isProducible() -> producers
            method.isConsumable() -> consumers
            else -> return
        }

        if (visible) {
            cache.visibleMethods.add(method)
        } else {
            cache.visibleMethods.remove(method)
        }
    }
}

/**
 * Cache of [Attribute]s.  Basically a [Consumer] cache or a [Producer] cache.  This supports 4 access patterns:
 * - by [AttributeContainer]
 * - by [WorkspaceComponent]
 * - by the [Type] which an attribute produces or consumes
 * - by [Attribute.visibility]
 */
sealed class AttributeCache<T : Attribute> {

    /**
     * All attributes.
     */
    private val all = HashSet<T>()

    /**
     * Map from [AttributeContainer]s to attributes.  E.g. neuron to its produers and connsumers, like
     * getActivationn, setActivaiton, getInputValue, etc.
     */
    private val byContainer = HashMap<AttributeContainer, HashSet<T>>()

    /**
     * Map from [WorkspaceComponent]s to attributes.
     */
    private val byComponent = HashMap<WorkspaceComponent, HashSet<T>>()

    /**
     * Map from [Type]s to attributes.
     */
    private val byType = HashMap<Type, HashSet<T>>()

    /**
     * Which attributes are visible
     */
    val visibleMethods = HashSet<Method>()

    /**
     * Get by type.
     */
    operator fun get(type: Type) = byType[type] ?: hashSetOf()

    /**
     * Get by attributecontainer
     */
    operator fun get(container: AttributeContainer) = byContainer[container] ?: hashSetOf()

    /**
     * Get by WorkspaceComponent
     */
    operator fun get(component: WorkspaceComponent) = byComponent[component] ?: hashSetOf()

    /**
     * Add a [AttributeContainer] to the cache.
     */
    abstract fun addContainer(component: WorkspaceComponent, container: AttributeContainer)

    /**
     * Add a [Consumer] or [Producer] to the cache.
     */
    protected fun addAttributes(component: WorkspaceComponent, attributes: Collection<T>) {

        all.addAll(attributes)

        attributes.groupBy { it.type }.forEach {

            if (it.key !in byType) {
                byType[it.key] = it.value.toHashSet()
            } else {
                byType[it.key]!!.addAll(it.value)
            }
        }

        attributes.groupBy { it.baseObject as AttributeContainer }.forEach {
            if (it.key !in byContainer) {
                byContainer[it.key] = it.value.toHashSet()
            } else {
                byContainer[it.key]!!.addAll(it.value)
            }
        }


        if (component in byComponent) {
            byComponent[component]!!.addAll(attributes)
        } else {
            byComponent[component] = attributes.toHashSet()
        }
    }

    /**
     * Remove an attribute container (must pass in workspace component)
     */
    fun removeContainer(component: WorkspaceComponent, attributeContainer: AttributeContainer) {
        // Get all the attributes(producers and consumers) associated with a conntainer
        val attributes = byContainer[attributeContainer]
        // Clean up all the maps that reference these attributes
        if (attributes != null) {
            all.removeAll(attributes)
            byType.values.forEach { it.removeAll(attributes) }
            byComponent[component]?.removeAll(attributes)
            byContainer.remove(attributeContainer)
        }
    }

}

/**
 * Producer cache.
 */
class ProducerCache : AttributeCache<Producer>() {
    override fun addContainer(component: WorkspaceComponent, container: AttributeContainer) {
        addAttributes(component, container.producers)
        container.producibles
                .filter { it !in visibleMethods }
                .filter { it.getAnnotation(Producible::class.java).defaultVisibility }
                .let { visibleMethods.addAll(it) }
    }
}

/**
 * Consumer cache.
 */
class ConsumerCache : AttributeCache<Consumer>() {
    override fun addContainer(component: WorkspaceComponent, container: AttributeContainer) {
        addAttributes(component, container.consumers)
        container.consumables
                .filter { it !in visibleMethods }
                .filter { it.getAnnotation(Consumable::class.java).defaultVisibility }
                .let { visibleMethods.addAll(it) }
    }
}