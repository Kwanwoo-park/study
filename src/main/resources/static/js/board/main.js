function fnLeft(listId, ImageArr) {
    console.log(ImageArr);
}

function fnRight(listId, ImageArr) {
    console.log(ImageArr);
    const main_image = document.getElementById('main_img' + listId);
    const img_id = document.getElementById('img' + listId);
    console.log(img_id.value);
    console.log(ImageArr[parseInt(img_id.value )+ 1])
    main_image.src = "/img/" + ImageArr[parseInt(img_id.value )+ 1].imgSrc;
    img_id.value = parseInt(img_id.value )+ 1;
}