const name = document.getElementById('name')
const container = document.querySelector('.container')

if (name) {
    name.addEventListener('keydown', (event) => {
        const field = document.querySelector('.list-group-flush')

        if (event.key == 'Enter') {
            if (field)
                field.remove();

            let area = document.createElement('ul');
            area.className = 'list-group-flush'

            container.append(area)

            fetch(`/api/member/search?name=` + name.value, {
                method: 'GET',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json['result'] > 0) {
                    json.list.forEach(data => {
                        let newArea = document.createElement('li');
                        newArea.className = 'list-group-item';

                        let span = document.createElement('span');

                        let img = document.createElement('img');
                        img.src = data.profile;
                        img.className = 'profile';
                        img.id = 'profile';

                        let memberHref = document.createElement('a');
                        memberHref.href = "/member/search/detail?email=" + data.email;
                        memberHref.innerText = data.name;

                        let email = document.createElement('span');
                        email.innerText = data.email;

                        span.append(img);
                        span.append(memberHref);
                        span.append(email);

                        newArea.append(span);

                        area.append(newArea);
                    })
                } else
                    alert("다시 시도하여주십시오")
            })
            .catch((error) => {
                console.error(error);
                alert("다시 시도하여주십시오")
            })
        }
    })
}