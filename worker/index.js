export default {
	async fetch(request, env, ctx) {
		const url = new URL(request.url);
		const path = url.pathname;

		if (request.method === 'OPTIONS') {
			return handleOptions(request);
		}

		try {
			if (path === '/auth' && request.method === 'POST') {
				return await handleAuth(request, env);
			}
			if (path === '/auth/offline' && request.method === 'POST') {
				return await handleAuthOffline(request, env);
			}
			if (path === '/profile' && request.method === 'PUT') {
				return await handleProfilePut(request, env);
			}
			if (path === '/profile' && request.method === 'DELETE') {
				return await handleProfileDelete(request, env);
			}
			if (path.startsWith('/profile/') && request.method === 'GET') {
				const uuid = path.slice('/profile/'.length);
				return await handleProfileGet(uuid, request, env);
			}
			if (path === '/profiles/batch' && request.method === 'POST') {
				return await handleProfilesBatch(request, env);
			}
if (path === '/report' && request.method === 'POST') {
			return await handleReport(request, env);
		}
		if (path.startsWith('/public/profile/') && request.method === 'GET') {
			const uuid = path.slice('/public/profile/'.length);
			return await handlePublicProfileGet(uuid, request, env);
		}
		if (path === '/stats' && request.method === 'GET') {
			return await handleStats(env);
		}

		return json({ error: 'Not found' }, 404);
		} catch (e) {
			console.error('Worker error:', e);
			return json({ error: 'Internal server error' }, 500);
		}
	}
};

const NODES = [
	{ id: 'eeur', binding: 'DB_EEUR', label: 'Europe', lat: 52.23, lon: 21.01 },
	{ id: 'wnam', binding: 'DB_WNAM', label: 'North America', lat: 37.34, lon: -121.89 },
	{ id: 'apac', binding: 'DB_APAC', label: 'Asia Pacific', lat: 1.35, lon: 103.82 }
];

function dbFor(env, node) {
	return env[node.binding];
}

function orderedNodes(request) {
	const cf = request.cf || {};
	const lat = Number(cf.latitude);
	const lon = Number(cf.longitude);
	if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
		return [...NODES];
	}
	return [...NODES].sort((a, b) => distance(lat, lon, a) - distance(lat, lon, b));
}

function distance(lat, lon, node) {
	const rad = Math.PI / 180;
	const dLat = (node.lat - lat) * rad;
	const dLon = (node.lon - lon) * rad;
	const a = Math.sin(dLat / 2) ** 2
		+ Math.cos(lat * rad) * Math.cos(node.lat * rad) * Math.sin(dLon / 2) ** 2;
	return 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

async function findProfile(request, env, uuid) {
	for (const node of orderedNodes(request)) {
		const row = await dbFor(env, node).prepare(
			'SELECT data, skin, updated FROM profiles WHERE uuid = ?'
		).bind(uuid).first();
		if (row) {
			return { node, row };
		}
	}
	return null;
}

async function handleStats(env) {
	const nodes = [];
	let total = 0;
	for (const node of NODES) {
		try {
			const row = await dbFor(env, node).prepare(
				'SELECT COUNT(*) AS count FROM profiles'
			).first();
			const count = Number(row ? row.count : 0);
			total += count;
			nodes.push({ id: node.id, label: node.label, count, ok: true });
		} catch (e) {
			nodes.push({ id: node.id, label: node.label, count: 0, ok: false });
		}
	}
	return json({ total, nodes }, 200, {
		'Access-Control-Allow-Origin': '*',
		'Cache-Control': 'no-store'
	});
}

function handleOptions(request) {
	const origin = request.headers.get('Origin');
	if (origin && isAllowedOrigin(origin)) {
		return new Response(null, {
			status: 204,
			headers: {
				'Access-Control-Allow-Origin': origin,
				'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
				'Access-Control-Allow-Headers': 'Content-Type, Authorization',
				'Access-Control-Max-Age': '86400',
			},
		});
	}
	return new Response(null, { status: 204 });
}

function isAllowedOrigin(origin) {
	try {
		const url = new URL(origin);
		return url.hostname === '127.0.0.1' || url.hostname === 'localhost';
	} catch {
		return false;
	}
}

async function handleAuth(request, env) {
	const body = await request.json();
	const { username, serverId } = body;

	if (!username || !serverId) {
		return json({ error: 'username and serverId required' }, 400);
	}
	if (typeof username !== 'string' || typeof serverId !== 'string') {
		return json({ error: 'Invalid parameters' }, 400);
	}
	if (!serverId.startsWith('jinfo-') || serverId.length < 38) {
		return json({ error: 'Invalid serverId format' }, 400);
	}
	if (!/^[a-zA-Z0-9_]{1,16}$/.test(username)) {
		return json({ error: 'Invalid username' }, 400);
	}

	const mojangUrl = `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(username)}&serverId=${encodeURIComponent(serverId)}`;
	const mojangRes = await fetch(mojangUrl);

	if (mojangRes.status === 204 || mojangRes.status === 403) {
		return json({ error: 'Invalid session' }, 401);
	}
	if (!mojangRes.ok) {
		return json({ error: 'Mojang session server error' }, 502);
	}

	const mojangData = await mojangRes.json();
	const uuid = mojangData.id;
	if (!uuid || !/^[0-9a-f]{32}$/.test(uuid)) {
		return json({ error: 'Invalid UUID from Mojang' }, 502);
	}

	const formattedUuid = formatUuid(uuid);
	const token = await createToken(formattedUuid, env);
	return json({ token, uuid: formattedUuid });
}

async function handleAuthOffline(request, env) {
	const body = await request.json();
	const { username, publicKey, timestamp, signature } = body;

	if (!username || !publicKey || !timestamp || !signature) {
		return json({ error: 'Missing required fields' }, 400);
	}
	if (typeof username !== 'string' || typeof publicKey !== 'string' ||
		typeof timestamp !== 'number' || typeof signature !== 'string') {
		return json({ error: 'Invalid parameter types' }, 400);
	}
	if (!/^[a-zA-Z0-9_]{1,16}$/.test(username)) {
		return json({ error: 'Invalid username' }, 400);
	}

	const now = Date.now();
	if (Math.abs(now - timestamp) > 120000) {
		return json({ error: 'Timestamp too old' }, 400);
	}

	const uuid = await generateOfflineUuid(username);
	const formattedUuid = formatUuid(uuid);

	const isValid = await verifyEd25519(publicKey, `${username}:${timestamp}`, signature);
	if (!isValid) {
		return json({ error: 'Invalid signature' }, 401);
	}

	const ordered = orderedNodes(request);
	let keyNode = null;
	let existing = null;
	for (const node of ordered) {
		const row = await dbFor(env, node).prepare(
			'SELECT pubkey FROM offline_keys WHERE uuid = ?'
		).bind(formattedUuid).first();
		if (row) {
			keyNode = node;
			existing = row;
			break;
		}
	}
	if (existing && existing.pubkey !== publicKey) {
		return json({ error: 'Public key mismatch for this username' }, 401);
	}
	if (!existing) {
		keyNode = ordered[0];
		await dbFor(env, keyNode).prepare(
			'INSERT INTO offline_keys (uuid, pubkey, created) VALUES (?, ?, ?)'
		).bind(formattedUuid, publicKey, Math.floor(now / 1000)).run();
	}

	const token = await createToken(formattedUuid, env);
	return json({ token, uuid: formattedUuid });
}

async function handleProfilePut(request, env) {
	const auth = await verifyToken(request, env);
	if (!auth) return json({ error: 'Unauthorized' }, 401);

	const body = await request.json();
	const { data, skin } = body;

	if (!data || typeof data !== 'string') {
		return json({ error: 'Data required' }, 400);
	}
	if (data.length > 500) {
		return json({ error: 'Data too long (max 500 chars)' }, 400);
	}

	const lines = data.split('\n');
	if (lines.length > 10) {
		return json({ error: 'Too many lines (max 10)' }, 400);
	}

	const linkCount = (data.match(/\{[a-z]+:/g) || []).length;
	if (linkCount > 6) {
		return json({ error: 'Too many links (max 6)' }, 400);
	}

	let skinVal = null;
	if (skin) {
		let parsed;
		if (typeof skin === 'string') {
			try {
				parsed = JSON.parse(skin);
			} catch (e) {
				parsed = null;
			}
		} else if (typeof skin === 'object') {
			parsed = skin;
		}
		if (parsed && parsed.url && typeof parsed.url === 'string' && parsed.url.startsWith('https://')) {
			skinVal = typeof skin === 'string' ? skin : JSON.stringify(skin);
		}
	}

	const cleaned = sanitizeProfileText(data);
	const encrypted = await encryptProfile(cleaned, env.AES_KEY);
	const now = Math.floor(Date.now() / 1000);

	const existingProfile = await findProfile(request, env, auth.uuid);
	const target = existingProfile ? existingProfile.node : orderedNodes(request)[0];
	const result = await dbFor(env, target).prepare(
		`INSERT INTO profiles (uuid, data, skin, updated) VALUES (?, ?, ?, ?)
		 ON CONFLICT(uuid) DO UPDATE SET data = excluded.data, skin = excluded.skin, updated = excluded.updated
		 WHERE updated < ?`
	).bind(auth.uuid, encrypted, skinVal, now, now - 60).run();

	if (result.meta.changes === 0) {
		return json({ error: 'Rate limited: wait 60 seconds between updates' }, 429);
	}

	return json({ success: true });
}

async function handleProfileDelete(request, env) {
	const auth = await verifyToken(request, env);
	if (!auth) return json({ error: 'Unauthorized' }, 401);

	const existing = await findProfile(request, env, auth.uuid);
	if (!existing) {
		return json({ error: 'Profile not found' }, 404);
	}
	await dbFor(env, existing.node).prepare('DELETE FROM profiles WHERE uuid = ?')
		.bind(auth.uuid).run();
	return json({ success: true });
}

async function handleProfileGet(uuid, request, env) {
	const auth = await verifyToken(request, env);
	if (!auth) return json({ error: 'Unauthorized' }, 401);

	if (!isValidUuid(uuid)) {
		return json({ error: 'Invalid UUID format' }, 400);
	}

	const found = await findProfile(request, env, uuid);
	if (!found) {
		return json({ error: 'Profile not found' }, 404);
	}

	const row = found.row;
	const decrypted = await decryptProfile(row.data, env.AES_KEY);
	const isVerified = isUuidV4(uuid);

	return json({
		uuid,
		data: decrypted,
		skin: row.skin,
		updated: row.updated,
		verified: isVerified
	});
}

async function handleProfilesBatch(request, env) {
	const auth = await verifyToken(request, env);
	if (!auth) return json({ error: 'Unauthorized' }, 401);

	const body = await request.json();
	const { uuids } = body;

	if (!Array.isArray(uuids) || uuids.length === 0 || uuids.length > 50) {
		return json({ error: 'Invalid uuids array (1-50)' }, 400);
	}

	const validUuids = uuids.filter(isValidUuid);
	if (validUuids.length === 0) {
		return json({ profiles: {} });
	}

	const placeholders = validUuids.map(() => '?').join(',');
	const profiles = {};
	for (const node of orderedNodes(request)) {
		const rows = await dbFor(env, node).prepare(
			`SELECT uuid, updated FROM profiles WHERE uuid IN (${placeholders})`
		).bind(...validUuids).all();
		for (const row of rows.results) {
			profiles[row.uuid] = {
				updated: row.updated,
				verified: isUuidV4(row.uuid)
			};
		}
	}

	return json({ profiles });
}

async function handleReport(request, env) {
	const auth = await verifyToken(request, env);
	if (!auth) return json({ error: 'Unauthorized' }, 401);

	const body = await request.json();
	const { profileUuid } = body;

	if (!profileUuid || !isValidUuid(profileUuid)) {
		return json({ error: 'Invalid profile UUID' }, 400);
	}
	if (profileUuid === auth.uuid) {
		return json({ error: 'Cannot report yourself' }, 400);
	}

	const found = await findProfile(request, env, profileUuid);
	if (!found) {
		return json({ error: 'Profile not found' }, 404);
	}

	const now = Math.floor(Date.now() / 1000);
	const dayAgo = now - 86400;
	const db = dbFor(env, found.node);

	const existing = await db.prepare(
		'SELECT 1 FROM reports WHERE profile_uuid = ? AND reporter_uuid = ? AND time > ?'
	).bind(profileUuid, auth.uuid, dayAgo).first();

	if (existing) {
		return json({ error: 'Already reported this profile today' }, 429);
	}

	await db.prepare(
		'INSERT INTO reports (profile_uuid, reporter_uuid, time) VALUES (?, ?, ?)'
	).bind(profileUuid, auth.uuid, now).run();

	return json({ success: true });
}

async function verifyToken(request, env) {
	const authHeader = request.headers.get('Authorization');
	if (!authHeader || !authHeader.startsWith('Bearer ')) {
		return null;
	}

	const token = authHeader.slice(7);
	const parts = token.split('.');
	if (parts.length !== 3) return null;

	const [uuid, expiry, hmac] = parts;
	if (!isValidUuid(uuid)) return null;

	const exp = parseInt(expiry, 10);
	if (isNaN(exp) || exp < Date.now() / 1000) return null;

	const expectedHmac = await hmacSha256(`${uuid}.${expiry}`, env.TOKEN_KEY);
	if (hmac !== expectedHmac) return null;

	return { uuid };
}

async function createToken(uuid, env) {
	const expiry = Math.floor(Date.now() / 1000) + 3600;
	const hmac = await hmacSha256(`${uuid}.${expiry}`, env.TOKEN_KEY);
	return `${uuid}.${expiry}.${hmac}`;
}

async function hmacSha256(data, key) {
	const keyData = new TextEncoder().encode(key);
	const cryptoKey = await crypto.subtle.importKey(
		'raw', keyData, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
	);
	const signature = await crypto.subtle.sign('HMAC', cryptoKey, new TextEncoder().encode(data));
	return Array.from(new Uint8Array(signature)).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function verifyEd25519(publicKeyB64, message, signatureB64) {
	try {
		const keyData = base64ToBytes(publicKeyB64);
		let publicKey;
		try {
			publicKey = await crypto.subtle.importKey(
				'spki',
				keyData,
				{ name: 'Ed25519' },
				false,
				['verify']
			);
		} catch {
			publicKey = await crypto.subtle.importKey(
				'raw',
				keyData,
				{ name: 'Ed25519' },
				false,
				['verify']
			);
		}
		return await crypto.subtle.verify(
			'Ed25519',
			publicKey,
			base64ToBytes(signatureB64),
			new TextEncoder().encode(message)
		);
	} catch {
		return false;
	}
}

function base64ToBytes(b64) {
	const binary = atob(b64);
	const bytes = new Uint8Array(binary.length);
	for (let i = 0; i < binary.length; i++) {
		bytes[i] = binary.charCodeAt(i);
	}
	return bytes;
}

async function generateOfflineUuid(username) {
	const data = `OfflinePlayer:${username}`;
	const hash = await md5(data);
	return hash.slice(0, 8) + hash.slice(8, 12) + '3' + hash.slice(13, 16) + '8' + hash.slice(17, 20) + hash.slice(20, 32);
}

function md5(str) {
	const utf8 = new TextEncoder().encode(str);
	return crypto.subtle.digest('MD5', utf8).then(buffer => {
		return Array.from(new Uint8Array(buffer)).map(b => b.toString(16).padStart(2, '0')).join('');
	});
}

function formatUuid(uuid) {
	return uuid.replace(/^([0-9a-f]{8})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{12})$/i, '$1-$2-$3-$4-$5');
}

function isValidUuid(uuid) {
	return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(uuid);
}

function isUuidV4(uuid) {
	const parts = uuid.split('-');
	return parts[2].startsWith('4');
}

async function encryptProfile(text, aesKey) {
	const keyHash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(aesKey));
	const key = await crypto.subtle.importKey('raw', keyHash, { name: 'AES-GCM' }, false, ['encrypt']);
	const iv = crypto.getRandomValues(new Uint8Array(12));
	const encoded = new TextEncoder().encode(text);
	const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, encoded);
	const combined = new Uint8Array(iv.length + ciphertext.byteLength);
	combined.set(iv);
	combined.set(new Uint8Array(ciphertext), iv.length);
	return btoa(String.fromCharCode(...combined));
}

async function decryptProfile(b64, aesKey) {
	const keyHash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(aesKey));
	const key = await crypto.subtle.importKey('raw', keyHash, { name: 'AES-GCM' }, false, ['decrypt']);
	const combined = Uint8Array.from(atob(b64), c => c.charCodeAt(0));
	const iv = combined.slice(0, 12);
	const ciphertext = combined.slice(12);
	const decrypted = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, ciphertext);
	return new TextDecoder().decode(decrypted);
}

function sanitizeProfileText(text) {
	return text
		.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, '')
		.replace(/[\u200B-\u200D\uFEFF]/g, '')
		.replace(/[\u202A-\u202E]/g, '')
		.replace(/(.)\1{4,}/g, '$1$1$1$1');
}

async function handlePublicProfileGet(uuid, request, env) {
	if (!isValidUuid(uuid)) {
		return json({ error: 'Invalid UUID format' }, 400);
	}

	const found = await findProfile(request, env, uuid);
	if (!found) {
		return json({ error: 'Profile not found' }, 404);
	}

	const row = found.row;
	const decrypted = await decryptProfile(row.data, env.AES_KEY);
	const isVerified = isUuidV4(uuid);

	return json({
		uuid,
		data: decrypted,
		skin: row.skin,
		updated: row.updated,
		verified: isVerified
	});
}

function json(data, status = 200, extraHeaders = {}) {
	return new Response(JSON.stringify(data), {
		status,
		headers: { 'Content-Type': 'application/json', ...extraHeaders }
	});
}