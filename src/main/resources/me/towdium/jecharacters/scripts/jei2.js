function initializeCoreMod() {
    Java.type('net.minecraftforge.coremod.api.ASMAPI').loadFile('me/towdium/jecharacters/scripts/_lib.js');
    return {
        'jecharacters-jei2': {
            'target': {
                'type': 'METHOD',
                'class': 'mezz.jei.search.ElementSearch',
                'methodName': '<init>',
                'methodDesc': '()V'
            },
            'transformer': function (method) {
                transConstruct(method,
                    'mezz/jei/search/suffixtree/GeneralizedSuffixTree',
                    'me/towdium/jecharacters/utils/Match$FakeTree'
                );
                return method;
            }
        },
        'jecharacters-jei9-2': {
            'target': {
                'type': 'METHOD',
                'class': 'mezz.jei.search.ElementPrefixParser',
                'methodName': '<init>',
                'methodDesc': '()V'
            },
            'transformer': function (method) {
                transInvokeLambda(method,
                    'mezz/jei/core/search/suffixtree/GeneralizedSuffixTree',
                    '<init>',
                    '()V',
                    'me/towdium/jecharacters/utils/Match$FakeTree',
                    '<init>',
                    '()V'
                );
                return method;
            }
        }
    }
}