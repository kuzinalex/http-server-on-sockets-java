/*
 * Выполняет асинхронный GET запрос к серверу по указанному URL.
 * В случае успешного ответа вызывает функцию callback и передает 
 * ей экземпляр объекта XMLHttpRequest содержащий ответ от сервера.
 */
function invokeAjax(url, callback) {	
	/* 
	 * Создание объекта XMLHttpRequest 
	 */
	var xhr;
	// Проверяем, существует ли стандартный объект XMLHttpRequest
	if (window.XMLHttpRequest) {
		xhr = new XMLHttpRequest();
	// Проверяем, существует ли ActiveX реализация
	} else if (window.ActiveXObject) {
		alert("Самое время сменить броузер!"); // ... и это правда ;)
		xhr = new ActiveXObject("Msxml2.XMLHTTP");
	// Если создать экземпляр объекта XMLHttpRequest не удалось,
	// выбрасываем исключение
	} else {
		throw new Error("Ajax is not supported.");
	}
	/* 
	 * Конфигурирование 
	 */	
	xhr.open("GET", url);
	xhr.onreadystatechange = function() {
		if (xhr.readyState != 4) {
			return;
		}
		if (xhr.status >= 200 && 
			xhr.status < 300) {
			// Объект xhr передается целиком, 
			// тк может понадобиться ответ как в форме текста, 
			// так и в форме Xml
			callback(xhr);					
		} else {
			throw new Error('HTTP exception: ' + xhr.statusText);
		}
	}	
	/*
	 * Выполнение запроса
	 */	
	xhr.send(null);
}