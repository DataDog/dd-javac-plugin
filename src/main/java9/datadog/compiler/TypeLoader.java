package datadog.compiler;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

public class TypeLoader {

    public static JCTree.JCExpression loadType(Context context, Class<?> clazz) {
        Names names = Names.instance(context);
        Name annotationClassName = names.fromString(clazz.getName());

        Symtab symtab = Symtab.instance(context);
        Symbol.ClassSymbol annotationSymbol = symtab.enterClass(symtab.noModule, annotationClassName);

        TreeMaker maker = TreeMaker.instance(context);
        return maker.Type(annotationSymbol.type);
    }

}
