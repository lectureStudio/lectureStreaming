function autocomplete(input, users, autocompleteContainer) {
    hideAndClearAutocompleteContainer();

    input.addEventListener("input", function(event) {
        hideAndClearAutocompleteContainer();
        const inputValue = this.value;
        var shown = false;

        for (var i=0; i < users.length; i++) {
            if (users[i].substr(0, inputValue.length).toUpperCase() == inputValue.toUpperCase()) {
                if (! shown) {
                    autocompleteContainer.classList.add("show");
                    shown = true;
                }

                item = document.createElement("div");
                item.classList.add("dropdown-item");
                strongPref = document.createElement("strong");
                strongPref.innerHTML = users[i].substr(0, inputValue.length);

                normalPost = document.createElement("span");
                normalPost.innerHTML = users[i].substr(inputValue.length);

                inputWithValue = document.createElement("input");
                inputWithValue.setAttribute("type", "hidden");
                inputWithValue.setAttribute ("value", users[i]);

                item.appendChild(strongPref);
                item.appendChild(normalPost);
                item.appendChild(inputWithValue);

                item.addEventListener("click", function(event) {
                    console.log(this.lastChild.value);
                    input.value = this.lastChild.value;
                    hideAndClearAutocompleteContainer();
                })

                autocompleteContainer.appendChild(item);
            }
        }
    });

    function hideAndClearAutocompleteContainer() {
        while (autocompleteContainer.firstChild) {
            autocompleteContainer.removeChild(autocompleteContainer.lastChild);
        }
        autocompleteContainer.classList.remove("show");
    }

    document.addEventListener("click", function (e) {
        hideAndClearAutocompleteContainer();
    });
}