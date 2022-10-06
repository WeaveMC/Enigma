package cuchaz.enigma.translation.mapping.serde;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodArgMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;

import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class MappingIoConverter {
	public static MemoryMappingTree toMappingIo(EntryTree<EntryMapping> mappings) {
		MemoryMappingTree mappingTree = new MemoryMappingTree();
		mappingTree.visitNamespaces("intermediary", List.of("named"));

		for (EntryTreeNode<EntryMapping> node : mappings) {
			if (node.getEntry() instanceof ClassEntry) {
				writeClass(node, mappings, mappingTree);
			}
		}

		mappingTree.visitEnd();
		return mappingTree;
	}

	private static void writeClass(EntryTreeNode<EntryMapping> classNode, EntryMap<EntryMapping> oldMappingTree, MemoryMappingTree newMappingTree) {
		ClassEntry classEntry = (ClassEntry) classNode.getEntry();
		EntryMapping mapping = oldMappingTree.get(classEntry);
		Deque<String> parts = new LinkedList<>();

		newMappingTree.visitClass(classEntry.getFullName());
		newMappingTree.visitComment(MappedElementKind.CLASS, mapping.javadoc());

		do {
			mapping = oldMappingTree.get(classEntry);

			if (mapping != null && mapping.targetName() != null) {
				parts.addFirst(mapping.targetName());
			} else {
				parts.addFirst(classEntry.getName());
			}

			classEntry = classEntry.getOuterClass();
		} while (classEntry != null);

		String mappedName = String.join("$", parts);
		newMappingTree.visitDstName(MappedElementKind.CLASS, 0, mappedName);

		for (EntryTreeNode<EntryMapping> child : classNode.getChildNodes()) {
			Entry<?> entry = child.getEntry();

			if (entry instanceof FieldEntry) {
				writeField(child, newMappingTree);
			} else if (entry instanceof MethodEntry) {
				writeMethod(child, newMappingTree);
			}
		}
	}

	private static void writeField(EntryTreeNode<EntryMapping> fieldNode, MemoryMappingTree mappingTree) {
		if (fieldNode.getValue() == null || fieldNode.getValue().equals(EntryMapping.DEFAULT)) {
			return; // Shortcut
		}

		FieldEntry fieldEntry = ((FieldEntry) fieldNode.getEntry());
		mappingTree.visitField(fieldEntry.getName(), fieldEntry.getDesc().toString());

		EntryMapping fieldMapping = fieldNode.getValue();

		if (fieldMapping == null) {
			fieldMapping = EntryMapping.DEFAULT;
		}

		mappingTree.visitDstName(MappedElementKind.FIELD, 0, fieldMapping.targetName());
		mappingTree.visitComment(MappedElementKind.FIELD, fieldMapping.javadoc());
	}

	private static void writeMethod(EntryTreeNode<EntryMapping> methodNode, MemoryMappingTree mappingTree) {
		MethodEntry methodEntry = ((MethodEntry) methodNode.getEntry());
		mappingTree.visitMethod(methodEntry.getName(), methodEntry.getDesc().toString());

		EntryMapping methodMapping = methodNode.getValue();

		if (methodMapping == null) {
			methodMapping = EntryMapping.DEFAULT;
		}

		mappingTree.visitDstName(MappedElementKind.METHOD, 0, methodMapping.targetName());
		mappingTree.visitComment(MappedElementKind.METHOD, methodMapping.javadoc());

		for (EntryTreeNode<EntryMapping> child : methodNode.getChildNodes()) {
			Entry<?> entry = child.getEntry();

			if (entry instanceof LocalVariableEntry) {
				writeMethodArg(child, mappingTree);
			}
		}
	}

	private static void writeMethodArg(EntryTreeNode<EntryMapping> methodArgNode, MemoryMappingTree mappingTree) {
		if (methodArgNode.getValue() == null || methodArgNode.getValue().equals(EntryMapping.DEFAULT)) {
			return; // Shortcut
		}

		LocalVariableEntry methodArgEntry = ((LocalVariableEntry) methodArgNode.getEntry());
		mappingTree.visitMethodArg(-1, methodArgEntry.getIndex(), methodArgEntry.getName());

		EntryMapping methodArgMapping = methodArgNode.getValue();

		if (methodArgMapping == null) {
			methodArgMapping = EntryMapping.DEFAULT;
		}

		mappingTree.visitDstName(MappedElementKind.METHOD_ARG, 0, methodArgMapping.targetName());
		mappingTree.visitComment(MappedElementKind.METHOD_ARG, methodArgMapping.javadoc());
	}

	public static EntryTree<EntryMapping> fromMappingIo(MemoryMappingTree mappingTree) {
		EntryTree<EntryMapping> dstMappingTree = new HashEntryTree<>();

		for (ClassMapping classMapping : mappingTree.getClasses()) {
			readClass(classMapping, dstMappingTree);
		}

		return dstMappingTree;
	}

	private static void readClass(ClassMapping classMapping, EntryTree<EntryMapping> mappingTree) {
		ClassEntry currentClass = new ClassEntry(classMapping.getSrcName());
		String dstName = classMapping.getDstName(0);

		if (dstName != null) {
			dstName = dstName.substring(dstName.lastIndexOf('$') + 1);
		}

		mappingTree.insert(currentClass, new EntryMapping(dstName));

		for (FieldMapping fieldMapping : classMapping.getFields()) {
			readField(fieldMapping, currentClass, mappingTree);
		}

		for (MethodMapping methodMapping : classMapping.getMethods()) {
			readMethod(methodMapping, currentClass, mappingTree);
		}
	}

	private static void readField(FieldMapping fieldMapping, ClassEntry parent, EntryTree<EntryMapping> mappingTree) {
		mappingTree.insert(new FieldEntry(parent, fieldMapping.getSrcName(), new TypeDescriptor(fieldMapping.getSrcDesc())),
				new EntryMapping(fieldMapping.getDstName(0)));
	}

	private static void readMethod(MethodMapping methodMapping, ClassEntry parent, EntryTree<EntryMapping> mappingTree) {
		MethodEntry currentMethod;
		mappingTree.insert(currentMethod = new MethodEntry(parent, methodMapping.getSrcName(), new MethodDescriptor(methodMapping.getSrcDesc())),
				new EntryMapping(methodMapping.getDstName(0)));

		for (MethodArgMapping methodArgMapping : methodMapping.getArgs()) {
			readMethodArg(methodArgMapping, currentMethod, mappingTree);
		}
	}

	private static void readMethodArg(MethodArgMapping methodArgMapping, MethodEntry parent, EntryTree<EntryMapping> mappingTree) {
		String methodArgSrcName = methodArgMapping.getSrcName() != null ? methodArgMapping.getSrcName() : "";

		mappingTree.insert(new LocalVariableEntry(parent, methodArgMapping.getLvIndex(), methodArgSrcName, true, null),
				new EntryMapping(methodArgMapping.getDstName(0)));
	}
}
