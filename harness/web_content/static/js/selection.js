$(document).ready(function() {
	$('#select-all-button').on('click',function() {
		$('.selectionThumbnail:checkbox').each(function() {
			var checked = this.checked;
            		this.checked = !checked;                        
        	});
	});
});
