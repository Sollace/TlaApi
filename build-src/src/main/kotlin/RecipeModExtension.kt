
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty

abstract class RecipeModExtension {
    
    abstract val modid : Property<String>
    
    abstract val displayname : Property<String>
    
    abstract val entrypoints : MapProperty<String, String>

    abstract val mixins : ListProperty<String>
}
