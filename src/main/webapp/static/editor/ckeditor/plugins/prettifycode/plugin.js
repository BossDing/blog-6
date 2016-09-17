CKEDITOR.plugins.add( 'prettifycode', {
	icons: 'prettifycode',
	init: function( editor ) {
		editor.addCommand( 'code', new CKEDITOR.dialogCommand( 'codeDialog' ) );
		// Create the toolbar button that executes the above command.
		editor.ui.addButton( 'Prettifycode', {
			label: '插入代码',
			command: 'code',
			toolbar: 'insert'
		});

        CKEDITOR.dialog.add( 'codeDialog', this.path + 'dialogs/code.js' );
	}
});
