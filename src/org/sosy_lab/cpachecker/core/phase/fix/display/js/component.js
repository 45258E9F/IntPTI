// initialization
$(document).ready(function() {
    $('.ui.dropdown').dropdown({
        on: 'click',
        onChange: selectDropItem
    });
});

function selectDropItem() {
    var selected = $('#mode_selected').text();
    if (selected === 'Global') {
        // global mode: current fix items are set to active
        $('.circular.ui.button').addClass('active');
    } else if (selected === 'Manual') {
        // manual mode: current fix items are set to inactive, then send a POST request to the server to reset the selected fixes
        $('.circular.ui.button').removeClass('active');
        $.post("http://localhost:9026", { op: 'clear' });
    }
}