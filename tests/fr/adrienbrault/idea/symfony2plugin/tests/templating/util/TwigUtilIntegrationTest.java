package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementFactory;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class TwigUtilIntegrationTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles(
            "app/Resources/TwigUtilIntegrationBundle/views/layout.html.twig",
            "app/Resources/TwigUtilIntegrationBundle/views/Foo/layout.html.twig",
            "app/Resources/TwigUtilIntegrationBundle/views/Foo/Bar/layout.html.twig"
        );
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateNameByOverwrite
     */
    public void testTemplateOverwriteNameGeneration() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertEquals(
            "TwigUtilIntegrationBundle:layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/layout.html.twig".split("/")))
        );

        assertEquals(
            "TwigUtilIntegrationBundle:Foo/layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/Foo/layout.html.twig".split("/")))
        );

        assertEquals(
            "TwigUtilIntegrationBundle:Foo/Bar/layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/Foo/Bar/layout.html.twig".split("/")))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateNameByOverwrite
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateName
     */
    public void testTemplateOverwriteNavigation() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:layout.html.twig' %}", "/views/layout.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:Foo/layout.html.twig' %}", "/views/Foo/layout.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:Foo/Bar/layout.html.twig' %}", "/views/Foo/Bar/layout.html.twig");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#isValidTemplateString
     */
    public void testIsValidTemplateString() {
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo/#{segment.typeKey}.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo/#{1 + 2}.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include ~ \"foo.html.twig\" ~ %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo.html.twig\" ~ %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include ~ \"foo.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));

        assertTrue(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
    }
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getDomainTrans
     */
    public void testGetDomainTrans() {
        String[] blocks = {
            "{{ '<caret>'|transchoice(3, {}, 'foo') }}",
            "{{ '<caret>'|transchoice(3, [], 'foo') }}",
            "{{ '<caret>'|trans({}, 'foo') }}",
            "{{ '<caret>'|trans([], 'foo') }}",
            //@TODO "{{ '<caret>'|trans(, 'foo') }}",
            "{{ '<caret>'|trans({'foo': 'foo', 'foo'}, 'foo') }}",
            "{{ '<caret>' | transchoice(count, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(c, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(, {'%var%': value}, 'foo') }}"
        };

        for (String s : blocks) {
            myFixture.configureByText(TwigFileType.INSTANCE, s);

            PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
            assertNotNull(element);

            assertEquals("foo", TwigUtil.getDomainTrans(element));
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getCreateAbleTemplatePaths
     */
    public void testGetCreateAbleTemplatePaths() {
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.copyFileToProject("dummy.html.twig", "res/dummy.html.twig");
        myFixture.copyFileToProject("dummy.html.twig", "res/foo/dummy.html.twig");

        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "@foo/bar.html.twig"), "src/res/bar.html.twig");
        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "bar.html.twig"), "src/res/bar.html.twig");

        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "FooBundle:Bar:dummy.html.twig"), "src/res/Bar/dummy.html.twig");
        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "FooBundle:Bar\\Foo:dummy.html.twig"), "src/res/Bar/Foo/dummy.html.twig");
        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "FooBundle:Bar:Foo\\dummy.html.twig"), "src/res/Bar/Foo/dummy.html.twig");

        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "@FooBundle/Bar/dummy.html.twig"), "src/res/Bar/dummy.html.twig");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScope
     */
    public void testGetTwigFileTransDefaultDomainForFileScope() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "{% trans_default_domain \"foo\" %}{{ <caret> }}");
        PsiElement psiElement = psiFile.findElementAt(myFixture.getCaretOffset());

        assertNotNull(psiElement);
        assertEquals("foo", TwigUtil.getTransDefaultDomainOnScope(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScope
     */
    public void testGetTwigFileTransDefaultDomainForEmbedScope() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "{% embed 'default/e.html.twig' %}\n" +
            "  {% trans_default_domain \"foobar\" %}\n" +
            "  {{ <caret> }}\n" +
            "{% endembed %}\n"
        );

        PsiElement psiElement = psiFile.findElementAt(myFixture.getCaretOffset());

        assertNotNull(psiElement);
        assertEquals("foobar", TwigUtil.getTransDefaultDomainOnScope(psiElement));
    }

    private PsiElement createPsiElementAndFindString(@NotNull String content, @NotNull IElementType type) {
        PsiElement psiElement = TwigElementFactory.createPsiElement(getProject(), content, type);
        if(psiElement == null) {
            fail();
        }

        final PsiElement[] string = {null};
        psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (string[0] == null && element.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
                    string[0] = element;
                }
                super.visitElement(element);
            }
        });

        return string[0];
    }

}
