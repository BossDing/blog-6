CKEDITOR.dialog.add( 'codeDialog', function( editor ) {
    return {
        title: 'Google Code Prettify',
        minWidth: 400,
        minHeight: 200,

        contents: [
            {
                id: 'tab-basic',
                elements: [
                    {
                		type: 'select',
                	    id: 'code-type',
                	    label: '选择语言',
                	    width:'100%',
                	    style:'width:100%',
                	    items: [['basic'],['css'],['go'],[ 'html' ], [ 'java' ],[ 'lua' ], [ 'sql' ], [ 'vb' ], [ 'xml' ],['其他'] ],
                	    'default': 'java'
                    },
                    {
                        type: 'textarea',
                        id: 'content',
                        label: '粘贴文本',
                       	 rows:20,
                        validate: CKEDITOR.dialog.validate.notEmpty( "内容不能为空" )
                    }
                ]
            }
        ],
        onOk: function() {
        	var codeType = this.getValueOf('tab-basic','code-type');
        	var codeCss = 'prettyprint '+(codeType == '其他' ? '' : 'lang-'+codeType);
        	var v = this.getValueOf('tab-basic','content');
        	editor.insertHtml('<pre class="'+codeCss+'">');
        	editor.insertText(v);
        	editor.insertHtml('</pre>');
        }
    };
});
