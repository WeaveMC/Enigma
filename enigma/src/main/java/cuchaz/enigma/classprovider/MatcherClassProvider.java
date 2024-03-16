package cuchaz.enigma.classprovider;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import matcher.model.type.ClassEnvironment;
import org.objectweb.asm.tree.ClassNode;

public class MatcherClassProvider implements ClassProvider {
	private final ClassEnvironment env;
	private Set<String> classNames;

	public MatcherClassProvider(ClassEnvironment env) {
		this.env = env;
	}

	@Override
	@Nullable
	public ClassNode get(String name) {
		return env.getClsByNameA(name).getMergedAsmNode();
	}

	public Set<String> getClassNames() {
		if (classNames == null) {
			classNames = new HashSet<>(env.getEnvA().getClasses()
					.stream()
					.filter(cls -> cls.isReal())
					.map(cls -> cls.getName())
					.toList());
		}

		return classNames;
	}
}
