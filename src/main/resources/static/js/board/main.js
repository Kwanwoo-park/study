function fnLeft(listId, ImageArr) {
    const main_image = document.getElementById('main_img' + listId);
    const img_id = document.getElementById('img' + listId);
    const left_arrow = document.getElementById('left' + listId);
    const right_arrow = document.getElementById('right' + listId);

    main_image.src = "/img/" + ImageArr[parseInt(img_id.value) - 1].imgSrc;
    img_id.value = parseInt(img_id.value) - 1;

    if (right_arrow.style.display === 'none')
        right_arrow.style.display = 'flex'

    if (parseInt(img_id.value) == 0)
        left_arrow.style.display = 'none'
}

function fnRight(listId, ImageArr) {
    const main_image = document.getElementById('main_img' + listId);
    const img_id = document.getElementById('img' + listId);
    const left_arrow = document.getElementById('left' + listId);
    const right_arrow = document.getElementById('right' + listId);

    main_image.src = "/img/" + ImageArr[parseInt(img_id.value)+ 1].imgSrc;
    img_id.value = parseInt(img_id.value)+ 1;

    if (left_arrow.style.display === 'none')
        left_arrow.style.display = 'flex'

    if (parseInt(img_id.value) == ImageArr.length-1)
        right_arrow.style.display = 'none'
}