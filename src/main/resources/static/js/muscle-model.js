import * as THREE from './vendor/three.bundle.js';
import { GLTFLoader, OrbitControls, MeshoptDecoder, RoomEnvironment } from './vendor/three.bundle.js';

// Camera presets and bounding centers
const HOME_FRONT = { pos: new THREE.Vector3(0, 0.90, 3.45), target: new THREE.Vector3(0, 0.86, 0.02) };
const HOME_BACK  = { pos: new THREE.Vector3(0, 0.90, -3.45), target: new THREE.Vector3(0, 0.86, 0.02) };

// Detailed anatomical framing targets computed from geometry
const DETAIL_TARGETS = {
    CHEST:                  { target: new THREE.Vector3(0, 1.29, 0.03),  pos: new THREE.Vector3(0, 1.32, 1.25) },
    CHEST_UPPER:            { target: new THREE.Vector3(0, 1.34, 0.04),  pos: new THREE.Vector3(0, 1.36, 1.05) },
    CHEST_MIDDLE:           { target: new THREE.Vector3(0, 1.30, 0.07),  pos: new THREE.Vector3(0, 1.30, 1.05) },
    CHEST_LOWER:            { target: new THREE.Vector3(0, 1.27, 0.07),  pos: new THREE.Vector3(0, 1.27, 1.05) },

    BACK:                   { target: new THREE.Vector3(0, 1.24, -0.01), pos: new THREE.Vector3(0, 1.24, -1.35) },
    BACK_UPPER:             { target: new THREE.Vector3(0, 1.36, -0.01), pos: new THREE.Vector3(0, 1.36, -1.15) },
    BACK_LATS:              { target: new THREE.Vector3(0, 1.14, -0.05), pos: new THREE.Vector3(0, 1.14, -1.15) },
    BACK_ERECTORS:          { target: new THREE.Vector3(0, 1.23, -0.04), pos: new THREE.Vector3(0, 1.23, -1.15) },

    SHOULDERS:              { target: new THREE.Vector3(0, 1.35, -0.02), pos: new THREE.Vector3(0.50, 1.38, 1.15) },
    SHOULDERS_FRONT:        { target: new THREE.Vector3(0, 1.36, 0.02),  pos: new THREE.Vector3(0, 1.36, 1.15) },
    SHOULDERS_SIDE:         { target: new THREE.Vector3(0, 1.35, -0.01), pos: new THREE.Vector3(0.95, 1.35, 0.35) },
    SHOULDERS_REAR:         { target: new THREE.Vector3(0, 1.34, -0.03), pos: new THREE.Vector3(0, 1.34, -1.15) },

    BICEPS:                 { target: new THREE.Vector3(0.24, 1.25, 0.02), pos: new THREE.Vector3(0.24, 1.25, 1.10) },
    BICEPS_LONG:            { target: new THREE.Vector3(0.25, 1.25, 0.01), pos: new THREE.Vector3(0.35, 1.25, 1.05) },
    BICEPS_SHORT:           { target: new THREE.Vector3(0.22, 1.27, 0.01), pos: new THREE.Vector3(0.20, 1.27, 1.05) },

    TRICEPS:                { target: new THREE.Vector3(0.24, 1.21, -0.03), pos: new THREE.Vector3(0.24, 1.21, -1.10) },
    TRICEPS_LONG:           { target: new THREE.Vector3(0.22, 1.24, -0.04), pos: new THREE.Vector3(0.22, 1.24, -1.05) },
    TRICEPS_LATERAL:        { target: new THREE.Vector3(0.28, 1.20, -0.03), pos: new THREE.Vector3(0.35, 1.20, -1.05) },
    TRICEPS_MEDIAL:         { target: new THREE.Vector3(0.25, 1.22, -0.02), pos: new THREE.Vector3(0.25, 1.22, -1.05) },

    CORE:                   { target: new THREE.Vector3(0, 1.12, 0.05),  pos: new THREE.Vector3(0, 1.12, 1.15) },
    CORE_RECTUS:            { target: new THREE.Vector3(0, 1.06, 0.08),  pos: new THREE.Vector3(0, 1.06, 1.05) },
    CORE_OBLIQUES:          { target: new THREE.Vector3(0, 1.08, 0.05),  pos: new THREE.Vector3(0.35, 1.08, 1.05) },
    CORE_TRANSVERSE:        { target: new THREE.Vector3(0, 1.13, 0.02),  pos: new THREE.Vector3(0, 1.13, 1.10) },

    GLUTES:                 { target: new THREE.Vector3(0, 0.89, -0.02), pos: new THREE.Vector3(0.25, 0.92, -1.15) },
    GLUTES_MAXIMUS:         { target: new THREE.Vector3(0, 0.88, -0.05), pos: new THREE.Vector3(0, 0.88, -1.10) },
    GLUTES_MEDIUS:          { target: new THREE.Vector3(0, 0.93, -0.01), pos: new THREE.Vector3(0.40, 0.95, -0.95) },
    GLUTES_MINIMUS:         { target: new THREE.Vector3(0, 0.91, -0.02), pos: new THREE.Vector3(0.35, 0.93, -0.95) },

    QUADRICEPS:             { target: new THREE.Vector3(0, 0.65, 0.03),  pos: new THREE.Vector3(0, 0.65, 1.30) },
    QUADRICEPS_RECTUS:      { target: new THREE.Vector3(0, 0.67, 0.04),  pos: new THREE.Vector3(0, 0.67, 1.15) },
    QUADRICEPS_LATERALIS:   { target: new THREE.Vector3(0, 0.63, 0.02),  pos: new THREE.Vector3(0.25, 0.63, 1.15) },
    QUADRICEPS_MEDIALIS:    { target: new THREE.Vector3(0, 0.62, 0.04),  pos: new THREE.Vector3(0, 0.62, 1.15) },
    QUADRICEPS_INTERMEDIUS: { target: new THREE.Vector3(0, 0.63, 0.03),  pos: new THREE.Vector3(0, 0.63, 1.15) },

    HAMSTRINGS:             { target: new THREE.Vector3(0, 0.60, -0.02), pos: new THREE.Vector3(0, 0.60, -1.30) },
    HAMSTRINGS_BICEPS:      { target: new THREE.Vector3(0, 0.62, -0.03), pos: new THREE.Vector3(0.20, 0.62, -1.15) },
    HAMSTRINGS_TENDINOSUS:  { target: new THREE.Vector3(0, 0.60, -0.02), pos: new THREE.Vector3(-0.15, 0.60, -1.15) },
    HAMSTRINGS_MEMBRANOSUS: { target: new THREE.Vector3(0, 0.61, -0.01), pos: new THREE.Vector3(-0.15, 0.61, -1.15) },

    CALVES:                 { target: new THREE.Vector3(0, 0.32, -0.03), pos: new THREE.Vector3(0, 0.32, -1.25) },
    CALVES_MEDIAL:          { target: new THREE.Vector3(0, 0.36, -0.03), pos: new THREE.Vector3(-0.15, 0.36, -1.10) },
    CALVES_LATERAL:         { target: new THREE.Vector3(0, 0.36, -0.04), pos: new THREE.Vector3(0.20, 0.36, -1.10) },
    CALVES_SOLEUS:          { target: new THREE.Vector3(0, 0.27, -0.02), pos: new THREE.Vector3(0, 0.27, -1.10) }
};

// Cutaway rules: which superficial muscles to make translucent when deep layers are selected
const CUTAWAY_FADE_REGIONS = {
    QUADRICEPS_INTERMEDIUS: ['QUADRICEPS_RECTUS'],
    HAMSTRINGS_MEMBRANOSUS: ['HAMSTRINGS_TENDINOSUS'],
    GLUTES_MINIMUS:         ['GLUTES_MAXIMUS', 'GLUTES_MEDIUS'],
    TRICEPS_MEDIAL:         ['TRICEPS_LONG', 'TRICEPS_LATERAL'],
    CALVES_SOLEUS:          ['CALVES_MEDIAL', 'CALVES_LATERAL'],
    CORE_TRANSVERSE:        ['CORE_OBLIQUES', 'CORE_RECTUS']
};

/** Classify each anatomical structure by name and layer into fitness group and region */
function classifyStructure(name, layer) {
    if (layer === 'skeleton') {
        return { group: 'SKELETON', region: '', label: name };
    }
    const l = name.toLowerCase();

    // CHEST
    if (l.includes('pectoralis major')) {
        if (l.includes('clavicular')) return { group: 'CHEST', region: 'CHEST_UPPER', label: 'Upper chest (clavicular head)' };
        if (l.includes('sternocostal')) return { group: 'CHEST', region: 'CHEST_MIDDLE', label: 'Middle chest (sternocostal head)' };
        if (l.includes('abdominal')) return { group: 'CHEST', region: 'CHEST_LOWER', label: 'Lower chest (abdominal head)' };
        return { group: 'CHEST', region: 'CHEST_MIDDLE', label: 'Pectoralis major' };
    }
    if (l.includes('pectoralis minor') || l.includes('subclavius')) {
        return { group: 'CHEST', region: 'CHEST_UPPER', label: l.includes('minor') ? 'Pectoralis minor' : 'Subclavius' };
    }
    if (l.includes('serratus anterior')) {
        return { group: 'CHEST', region: 'CHEST_LOWER', label: 'Serratus anterior' };
    }

    // SHOULDERS
    if (l.includes('deltoid')) {
        if (l.includes('clavicular') || l.includes('anterior')) return { group: 'SHOULDERS', region: 'SHOULDERS_FRONT', label: 'Anterior deltoid' };
        if (l.includes('acromial') || l.includes('lateral') || l.includes('middle')) return { group: 'SHOULDERS', region: 'SHOULDERS_SIDE', label: 'Middle deltoid' };
        if (l.includes('spinal') || l.includes('posterior')) return { group: 'SHOULDERS', region: 'SHOULDERS_REAR', label: 'Posterior deltoid' };
        return { group: 'SHOULDERS', region: 'SHOULDERS_SIDE', label: 'Deltoid' };
    }
    if (l.includes('supraspinatus') || l.includes('infraspinatus') || l.includes('teres minor') || l.includes('subscapularis')) {
        return { group: 'SHOULDERS', region: 'SHOULDERS_REAR', label: 'Rotator cuff' };
    }

    // BACK
    if (l.includes('trapezius')) {
        return { group: 'BACK', region: 'BACK_UPPER', label: 'Trapezius' };
    }
    if (l.includes('rhomboid') || l.includes('levator scapulae') || l.includes('teres major')) {
        return { group: 'BACK', region: 'BACK_UPPER', label: l.includes('rhomboid') ? 'Rhomboids' : (l.includes('teres major') ? 'Teres major' : 'Levator scapulae') };
    }
    if (l.includes('latissimus dorsi')) {
        return { group: 'BACK', region: 'BACK_LATS', label: 'Latissimus dorsi' };
    }
    if (l.includes('iliocostalis') || l.includes('longissimus') || l.includes('spinalis') || l.includes('multifidus') || l.includes('rotatores') || l.includes('splenius') || l.includes('serratus posterior') || l.includes('interspinales') || l.includes('thoracolumbar')) {
        return { group: 'BACK', region: 'BACK_ERECTORS', label: 'Erector spinae' };
    }

    // ARMS: BICEPS & FOREARMS
    if (l.includes('biceps brachii')) {
        if (l.includes('long head')) return { group: 'BICEPS', region: 'BICEPS_LONG', label: 'Biceps long head' };
        if (l.includes('short head')) return { group: 'BICEPS', region: 'BICEPS_SHORT', label: 'Biceps short head' };
        return { group: 'BICEPS', region: 'BICEPS_LONG', label: 'Biceps brachii' };
    }
    if (l.includes('brachialis') || l.includes('coracobrachialis')) {
        return { group: 'BICEPS', region: 'BICEPS_SHORT', label: l.includes('coracobrachialis') ? 'Coracobrachialis' : 'Brachialis' };
    }
    if (l.includes('brachioradialis') || l.includes('pronator') || l.includes('supinator') || l.includes('flexor carpi') || l.includes('extensor carpi') || l.includes('palmaris') || l.includes('flexor digitorum') || l.includes('extensor digitorum') || l.includes('flexor pollicis') || l.includes('extensor pollicis') || l.includes('abductor pollicis') || l.includes('extensor indicis') || l.includes('extensor digiti minimi') || l.includes('flexor digiti minimi') || l.includes('abductor digiti minimi') || l.includes('opponens') || (l.includes('lumbrical') || l.includes('interossei')) && l.includes('hand') || l.includes('retinaculum') && l.includes('wrist')) {
        return { group: 'BICEPS', region: 'BICEPS_LONG', label: 'Forearm' };
    }

    // TRICEPS
    if (l.includes('triceps brachii')) {
        if (l.includes('long head')) return { group: 'TRICEPS', region: 'TRICEPS_LONG', label: 'Triceps long head' };
        if (l.includes('lateral head')) return { group: 'TRICEPS', region: 'TRICEPS_LATERAL', label: 'Triceps lateral head' };
        if (l.includes('medial head')) return { group: 'TRICEPS', region: 'TRICEPS_MEDIAL', label: 'Triceps medial head' };
        return { group: 'TRICEPS', region: 'TRICEPS_LATERAL', label: 'Triceps brachii' };
    }
    if (l.includes('anconeus')) {
        return { group: 'TRICEPS', region: 'TRICEPS_LATERAL', label: 'Anconeus' };
    }

    // CORE
    if (l.includes('rectus abdominis') || l.includes('pyramidalis') || l.includes('linea alba')) {
        return { group: 'CORE', region: 'CORE_RECTUS', label: 'Rectus abdominis' };
    }
    if (l.includes('external oblique') || l.includes('internal oblique')) {
        return { group: 'CORE', region: 'CORE_OBLIQUES', label: l.includes('external') ? 'External oblique' : 'Internal oblique' };
    }
    if (l.includes('transversus abdominis') || l.includes('quadratus lumborum') || l.includes('intercostal') || l.includes('diaphragm')) {
        return { group: 'CORE', region: 'CORE_TRANSVERSE', label: 'Transversus abdominis' };
    }

    // GLUTES & HIPS
    if (l.includes('gluteus maximus')) {
        return { group: 'GLUTES', region: 'GLUTES_MAXIMUS', label: 'Gluteus maximus' };
    }
    if (l.includes('gluteus medius') || l.includes('tensor fasciae latae')) {
        return { group: 'GLUTES', region: 'GLUTES_MEDIUS', label: l.includes('tensor') ? 'Tensor fasciae latae' : 'Gluteus medius' };
    }
    if (l.includes('gluteus minimus') || l.includes('piriformis') || l.includes('obturator') || l.includes('gemellus') || l.includes('quadratus femoris')) {
        return { group: 'GLUTES', region: 'GLUTES_MINIMUS', label: 'Gluteus minimus & deep rotators' };
    }

    // QUADRICEPS & UPPER LEGS
    if (l.includes('rectus femoris')) {
        return { group: 'QUADRICEPS', region: 'QUADRICEPS_RECTUS', label: 'Rectus femoris' };
    }
    if (l.includes('vastus lateralis')) {
        return { group: 'QUADRICEPS', region: 'QUADRICEPS_LATERALIS', label: 'Vastus lateralis' };
    }
    if (l.includes('vastus medialis')) {
        return { group: 'QUADRICEPS', region: 'QUADRICEPS_MEDIALIS', label: 'Vastus medialis' };
    }
    if (l.includes('vastus intermedius') || l.includes('articularis genus')) {
        return { group: 'QUADRICEPS', region: 'QUADRICEPS_INTERMEDIUS', label: 'Vastus intermedius' };
    }
    if (l.includes('sartorius') || l.includes('pectineus') || l.includes('gracilis') || l.includes('adductor') || l.includes('iliotibial tract')) {
        return { group: 'QUADRICEPS', region: 'QUADRICEPS_RECTUS', label: 'Adductor / hip flexor' };
    }
    if (l.includes('psoas') || l.includes('iliacus')) {
        return { group: 'CORE', region: 'CORE_TRANSVERSE', label: 'Iliopsoas' };
    }

    // HAMSTRINGS
    if (l.includes('biceps femoris')) {
        return { group: 'HAMSTRINGS', region: 'HAMSTRINGS_BICEPS', label: 'Biceps femoris' };
    }
    if (l.includes('semitendinosus')) {
        return { group: 'HAMSTRINGS', region: 'HAMSTRINGS_TENDINOSUS', label: 'Semitendinosus' };
    }
    if (l.includes('semimembranosus')) {
        return { group: 'HAMSTRINGS', region: 'HAMSTRINGS_MEMBRANOSUS', label: 'Semimembranosus' };
    }

    // CALVES & LOWER LEGS
    if (l.includes('gastrocnemius')) {
        if (l.includes('medial head')) return { group: 'CALVES', region: 'CALVES_MEDIAL', label: 'Medial gastrocnemius' };
        if (l.includes('lateral head')) return { group: 'CALVES', region: 'CALVES_LATERAL', label: 'Lateral gastrocnemius' };
        return { group: 'CALVES', region: 'CALVES_MEDIAL', label: 'Gastrocnemius' };
    }
    if (l.includes('soleus')) {
        return { group: 'CALVES', region: 'CALVES_SOLEUS', label: 'Soleus' };
    }
    if (l.includes('plantaris') || l.includes('calcaneal tendon') || l.includes('tibialis') || l.includes('fibularis') || l.includes('peroneus') || l.includes('popliteus') || (l.includes('foot') || l.includes('hallucis') || l.includes('toe')) && !l.includes('bone') && !l.includes('phalanx') && !l.includes('metatarsal')) {
        return { group: 'CALVES', region: 'CALVES_MEDIAL', label: 'Lower leg & calf' };
    }

    // NECK & HEAD
    if (l.includes('splenius') || l.includes('scalenus') || l.includes('capitis') || l.includes('sternocleidomastoid') || l.includes('omohyoid') || l.includes('sternohyoid') || l.includes('sternothyroid') || l.includes('thyrohyoid')) {
        return { group: 'BACK', region: 'BACK_UPPER', label: 'Neck & cervical stabilizers' };
    }

    return { group: 'CORE', region: '', label: name };
}

export function createMuscleModel(canvas, groupOf, onHover, onSelect, onRotate) {
    if (!canvas) return null;

    // Check WebGL availability
    const gl = canvas.getContext('webgl2') || canvas.getContext('webgl');
    if (!gl) {
        console.warn('WebGL is not available in this browser.');
        return null;
    }

    // Setup Three.js WebGL renderer
    const renderer = new THREE.WebGLRenderer({
        canvas,
        antialias: true,
        alpha: false,
        powerPreference: 'high-performance'
    });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.05;

    // Scene & dark gym background matching --background: #171a17
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x171a17);

    // Setup PBR image-based lighting environment
    const pmrem = new THREE.PMREMGenerator(renderer);
    const roomEnv = new RoomEnvironment();
    scene.environment = pmrem.fromScene(roomEnv, 0.04).texture;
    scene.environmentIntensity = 0.85;

    // Studio directional lighting
    const keyLight = new THREE.DirectionalLight(0xfff5eb, 1.5);
    keyLight.position.set(2.2, 3.2, 2.8);
    scene.add(keyLight);

    const rimLight = new THREE.DirectionalLight(0xdde8f5, 0.9);
    rimLight.position.set(-2.5, 2.0, -2.5);
    scene.add(rimLight);

    const frontFill = new THREE.DirectionalLight(0xffffff, 0.35);
    frontFill.position.set(0, 1.0, 3.0);
    scene.add(frontFill);

    const lowFill = new THREE.DirectionalLight(0x425042, 0.3);
    lowFill.position.set(0, -1.0, 1.5);
    scene.add(lowFill);

    // Floor contact shadow disc
    const shadowCanvas = document.createElement('canvas');
    shadowCanvas.width = shadowCanvas.height = 256;
    const sCtx = shadowCanvas.getContext('2d');
    const grad = sCtx.createRadialGradient(128, 128, 0, 128, 128, 128);
    grad.addColorStop(0, 'rgba(0, 0, 0, 0.55)');
    grad.addColorStop(0.35, 'rgba(0, 0, 0, 0.35)');
    grad.addColorStop(0.8, 'rgba(0, 0, 0, 0.08)');
    grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
    sCtx.fillStyle = grad;
    sCtx.fillRect(0, 0, 256, 256);
    const shadowTex = new THREE.CanvasTexture(shadowCanvas);
    const shadowMesh = new THREE.Mesh(
        new THREE.PlaneGeometry(1.25, 0.95),
        new THREE.MeshBasicMaterial({ map: shadowTex, transparent: true, depthWrite: false })
    );
    shadowMesh.rotation.x = -Math.PI / 2;
    shadowMesh.position.set(0, 0.001, 0.02);
    scene.add(shadowMesh);

    // Floor subtle pedestal disc
    const pedestalMesh = new THREE.Mesh(
        new THREE.CylinderGeometry(0.52, 0.54, 0.006, 48),
        new THREE.MeshStandardMaterial({
            color: 0x1f241e,
            roughness: 0.85,
            metalness: 0.1
        })
    );
    pedestalMesh.position.set(0, 0.003, 0.02);
    scene.add(pedestalMesh);

    // Camera with 30-degree FOV for minimal perspective distortion
    const camera = new THREE.PerspectiveCamera(30, 1, 0.1, 20);
    camera.position.copy(HOME_FRONT.pos);

    // OrbitControls for natural 360-degree rotation and zoom
    const controls = new OrbitControls(camera, canvas);
    controls.target.copy(HOME_FRONT.target);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    controls.minDistance = 0.45;
    controls.maxDistance = 6.0;
    controls.minPolarAngle = Math.PI * 0.12;
    controls.maxPolarAngle = Math.PI * 0.88;
    controls.screenSpacePanning = true;

    // Render loop and state
    let needsRender = true;
    let flyAnim = null;
    function requestRender() { needsRender = true; }
    controls.addEventListener('change', () => {
        requestRender();
    });

    // Model structures and materials
    const meshes = [];
    const meshesByGroup = new Map();
    const meshesByRegion = new Map();
    const meshByName = new Map();
    let baseMaterials = null;
    let stateMaterials = null;

    let selectedKey = '';
    let hoveredMesh = null;
    let currentView = 'front';
    let currentDetailGroup = '';
    let currentLayerMode = 'muscles'; // 'muscles', 'cutaway', 'skeleton', 'both'

    // Raycasting and pointer tracking
    const raycaster = new THREE.Raycaster();
    const pointerNdc = new THREE.Vector2();
    let pointerDownPos = null;
    let isDragging = false;
    let hoverThrottleTimer = 0;

    // Loading indicator element in stage
    const stageContainer = canvas.parentElement;
    let loadingEl = stageContainer?.querySelector('[data-model-loading]');
    if (!loadingEl && stageContainer) {
        loadingEl = document.createElement('div');
        loadingEl.className = 'muscle-stage-loading';
        loadingEl.setAttribute('data-model-loading', '');
        loadingEl.innerHTML = '<div class="stage-spinner" aria-hidden="true"></div><span class="stage-loading-text">Loading 3D anatomy...</span>';
        stageContainer.appendChild(loadingEl);
    }

    // Load Écorché compressed model
    const loader = new GLTFLoader();
    loader.setMeshoptDecoder(MeshoptDecoder);

    loader.load(
        '/models/ecorche/ecorche.meshopt.glb',
        (gltf) => {
            scene.add(gltf.scene);

            const rawMaterials = {};
            gltf.scene.traverse((o) => {
                if (!o.isMesh) return;
                const matName = o.material.name;
                rawMaterials[matName] = o.material;
                o.userData.baseMatName = matName;
                o.userData.originalMaterial = o.material;

                const name = o.userData.name || o.name;
                const layer = o.userData.layer;
                const info = classifyStructure(name, layer);
                o.userData.group = info.group;
                o.userData.region = info.region;
                o.userData.label = info.label;

                meshes.push(o);
                meshByName.set(name, o);

                if (info.group) {
                    if (!meshesByGroup.has(info.group)) meshesByGroup.set(info.group, []);
                    meshesByGroup.get(info.group).push(o);
                }
                if (info.region) {
                    if (!meshesByRegion.has(info.region)) meshesByRegion.set(info.region, []);
                    meshesByRegion.get(info.region).push(o);
                }
            });

            // Prepare specialized materials
            const baseMuscle = rawMaterials.Muscle || new THREE.MeshStandardMaterial({ color: 0xb54035, roughness: 0.48 });
            const baseBone = rawMaterials.Bone || new THREE.MeshStandardMaterial({ color: 0xded8ce, roughness: 0.62 });

            // Outer superficial muscle material with support for transparency during cutaway
            const outerMuscle = baseMuscle.clone();
            outerMuscle.name = 'OuterMuscle';

            // Selected material: vibrant brand lime-green accent (#c5e777)
            const pickMuscle = new THREE.MeshStandardMaterial({
                color: 0xc5e777,
                emissive: 0x3e5e1e,
                emissiveIntensity: 0.42,
                roughness: 0.38,
                metalness: 0.05,
                vertexColors: false
            });

            const pickBone = new THREE.MeshStandardMaterial({
                color: 0xd8ee9b,
                emissive: 0x3e5e1e,
                emissiveIntensity: 0.35,
                roughness: 0.5,
                metalness: 0.0,
                vertexColors: false
            });

            // Hover material: subtle lime/sage emissive glow preserving anatomical muscle colors
            const hoverMuscle = baseMuscle.clone();
            hoverMuscle.emissive = new THREE.Color(0x3d5a2b);
            hoverMuscle.emissiveIntensity = 0.35;

            const hoverOuter = outerMuscle.clone();
            hoverOuter.emissive = new THREE.Color(0x3d5a2b);
            hoverOuter.emissiveIntensity = 0.35;

            const hoverBone = baseBone.clone();
            hoverBone.emissive = new THREE.Color(0x3d5a2b);
            hoverBone.emissiveIntensity = 0.35;

            // Cutaway / translucent material for covering superficial muscles
            const cutawayMuscle = new THREE.MeshStandardMaterial({
                color: 0x8a3832,
                transparent: true,
                opacity: 0.16,
                depthWrite: false,
                roughness: 0.72,
                metalness: 0.0,
                vertexColors: false
            });

            baseMaterials = {
                Muscle: baseMuscle,
                OuterMuscle: outerMuscle,
                Bone: baseBone
            };

            stateMaterials = {
                hover: {
                    Muscle: hoverMuscle,
                    OuterMuscle: hoverOuter,
                    Bone: hoverBone
                },
                pick: {
                    Muscle: pickMuscle,
                    OuterMuscle: pickMuscle,
                    Bone: pickBone
                },
                cutaway: cutawayMuscle
            };

            // Assign OuterMuscle material to all superficial muscles
            for (const m of meshes) {
                if (m.userData.layer === 'superficial muscles' && m.userData.baseMatName === 'Muscle') {
                    m.userData.baseMatName = 'OuterMuscle';
                    m.material = outerMuscle;
                }
            }

            // Apply layer visibility (muscles only by default, skeleton hidden)
            applyLayerVisibility();

            // Apply queued selection & view
            applySelection();
            if (currentView && currentView !== 'front') {
                applyView(currentView, currentDetailGroup, selectedKey);
            }

            // Fade out loading indicator
            if (loadingEl) {
                loadingEl.classList.add('is-loaded');
                setTimeout(() => loadingEl.remove(), 450);
            }

            requestRender();
        },
        (progress) => {
            if (progress.total && loadingEl) {
                const percent = Math.round((progress.loaded / progress.total) * 100);
                const text = loadingEl.querySelector('.stage-loading-text');
                if (text) text.textContent = `Loading 3D anatomy (${percent}%)...`;
            }
        },
        (error) => {
            console.error('Failed to load anatomy GLB model:', error);
            if (loadingEl) {
                const text = loadingEl.querySelector('.stage-loading-text');
                if (text) text.textContent = 'Unable to load 3D anatomy model.';
            }
        }
    );

    // Layer visibility management
    function applyLayerVisibility() {
        const isSkeletonView = currentLayerMode === 'skeleton';
        const isBoth = currentLayerMode === 'both';
        const isCutaway = currentLayerMode === 'cutaway' || canvas.dataset.layer === 'cutaway';

        for (const m of meshes) {
            const layer = m.userData.layer;
            if (layer === 'skeleton') {
                m.visible = isSkeletonView || isBoth;
            } else if (layer === 'superficial muscles') {
                m.visible = !isSkeletonView;
            } else if (layer === 'deep muscles') {
                m.visible = !isSkeletonView || isBoth || isCutaway;
            }
        }
    }

    // Material updates based on selection, hover, and cutaways
    function applySelection() {
        if (!stateMaterials) return;

        const isCutaway = Boolean(selectedKey && CUTAWAY_FADE_REGIONS[selectedKey]);
        canvas.dataset.layer = isCutaway ? 'cutaway' : (currentLayerMode === 'cutaway' ? 'cutaway' : 'surface');
        applyLayerVisibility();

        const fadeRegions = isCutaway ? CUTAWAY_FADE_REGIONS[selectedKey] : null;

        for (const m of meshes) {
            const isSelected = isMeshSelected(m, selectedKey);
            const isHovered = m === hoveredMesh;
            const baseKey = m.userData.baseMatName || 'Muscle';

            if (isSelected) {
                m.material = stateMaterials.pick[baseKey] || stateMaterials.pick.Muscle;
            } else if (isHovered) {
                m.material = stateMaterials.hover[baseKey] || stateMaterials.hover.Muscle;
            } else if (isCutaway && fadeRegions && fadeRegions.includes(m.userData.region)) {
                m.material = stateMaterials.cutaway;
            } else {
                m.material = baseMaterials[baseKey] || baseMaterials.Muscle;
            }
        }

        requestRender();
    }

    function isMeshSelected(mesh, key) {
        if (!key) return false;
        // Region key match
        if (mesh.userData.region === key) return true;
        // Group key match
        if (mesh.userData.group === key) return true;
        return false;
    }

    // Smooth camera transition (flyTo)
    function flyTo(targetPos, targetLookAt, duration = 850) {
        flyAnim = {
            start: performance.now(),
            duration,
            fromPos: camera.position.clone(),
            toPos: targetPos.clone(),
            fromTarget: controls.target.clone(),
            toTarget: targetLookAt.clone()
        };
        requestRender();
    }

    function applyView(view, group, key) {
        currentView = view;
        currentDetailGroup = group;

        if (view === 'skeleton') {
            currentLayerMode = 'skeleton';
            applyLayerVisibility();
            flyTo(HOME_FRONT.pos, HOME_FRONT.target);
            return;
        }

        if (currentLayerMode === 'skeleton') {
            currentLayerMode = 'muscles';
            applyLayerVisibility();
        }

        if (view === 'front') {
            flyTo(HOME_FRONT.pos, HOME_FRONT.target);
        } else if (view === 'back') {
            flyTo(HOME_BACK.pos, HOME_BACK.target);
        } else if (view === 'detail') {
            // Find appropriate camera target: region first, then group, then default chest
            const targetKey = key || group || 'CHEST';
            const preset = DETAIL_TARGETS[targetKey] || DETAIL_TARGETS[group] || DETAIL_TARGETS.CHEST;
            flyTo(preset.pos, preset.target);
        }
    }

    // Picking via raycaster
    function pickMesh(clientX, clientY) {
        const rect = canvas.getBoundingClientRect();
        if (clientX < rect.left || clientX > rect.right || clientY < rect.top || clientY > rect.bottom) {
            return null;
        }

        pointerNdc.x = ((clientX - rect.left) / rect.width) * 2 - 1;
        pointerNdc.y = -((clientY - rect.top) / rect.height) * 2 + 1;

        raycaster.setFromCamera(pointerNdc, camera);

        // Raycast against visible meshes only, ignoring heavily faded cutaways
        const candidateMeshes = meshes.filter(m => m.visible && (!m.material.transparent || m.material.opacity > 0.35));
        const intersects = raycaster.intersectObjects(candidateMeshes, false);

        if (intersects.length > 0) {
            return intersects[0].object;
        }
        return null;
    }

    // Pointer events on canvas
    canvas.addEventListener('pointerdown', (e) => {
        if (e.button !== 0) return;
        pointerDownPos = { x: e.clientX, y: e.clientY };
        isDragging = false;
    });

    canvas.addEventListener('pointermove', (e) => {
        if (pointerDownPos) {
            const dist = Math.hypot(e.clientX - pointerDownPos.x, e.clientY - pointerDownPos.y);
            if (dist > 6) {
                isDragging = true;
                onRotate();
            }
        }

        // Throttle hover raycasting when not dragging
        if (!isDragging && e.pointerType === 'mouse' && !e.buttons) {
            clearTimeout(hoverThrottleTimer);
            hoverThrottleTimer = setTimeout(() => {
                const hit = pickMesh(e.clientX, e.clientY);
                if (hit !== hoveredMesh) {
                    hoveredMesh = hit;
                    canvas.style.cursor = hit ? 'pointer' : '';

                    if (hit) {
                        const targetKey = currentDetailGroup ? (hit.userData.region || hit.userData.group) : hit.userData.group;
                        onHover(targetKey || hit.userData.label);
                    } else {
                        onHover('');
                    }
                    applySelection();
                }
            }, 35);
        }
    });

    canvas.addEventListener('pointerup', (e) => {
        if (!pointerDownPos) return;
        const dist = Math.hypot(e.clientX - pointerDownPos.x, e.clientY - pointerDownPos.y);

        if (dist <= 6 && !isDragging) {
            // Click / tap detected
            const hit = pickMesh(e.clientX, e.clientY);
            if (hit) {
                // In detail mode or when clicking a specific region, prefer region key
                const hitRegion = hit.userData.region;
                const hitGroup = hit.userData.group;
                const chosen = (currentView === 'detail' && hitRegion) ? hitRegion : (hitGroup || hitRegion || '');
                if (chosen) {
                    onSelect(chosen);
                }
            } else {
                // Clicked background empty space -> deselect to all muscles
                onSelect('');
            }
        }

        pointerDownPos = null;
        isDragging = false;
    });

    canvas.addEventListener('pointerleave', () => {
        pointerDownPos = null;
        isDragging = false;
        if (hoveredMesh) {
            hoveredMesh = null;
            canvas.style.cursor = '';
            onHover('');
            applySelection();
        }
    });

    // Double-click to reset view to home
    canvas.addEventListener('dblclick', () => {
        flyTo(HOME_FRONT.pos, HOME_FRONT.target);
    });

    // Responsive canvas resizing
    function handleResize() {
        const width = canvas.clientWidth;
        const height = canvas.clientHeight;
        if (!width || !height) return;

        const pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
        renderer.setPixelRatio(pixelRatio);
        renderer.setSize(width, height, false);

        camera.aspect = width / height;
        camera.updateProjectionMatrix();
        requestRender();
    }

    const resizeObserver = new ResizeObserver(handleResize);
    resizeObserver.observe(canvas);
    handleResize();

    // Render-on-demand animation loop
    renderer.setAnimationLoop(() => {
        if (flyAnim) {
            const elapsed = performance.now() - flyAnim.start;
            const t = Math.min(1, elapsed / flyAnim.duration);
            // Cubic ease-out
            const ease = 1 - Math.pow(1 - t, 3);
            camera.position.lerpVectors(flyAnim.fromPos, flyAnim.toPos, ease);
            controls.target.lerpVectors(flyAnim.fromTarget, flyAnim.toTarget, ease);

            if (t >= 1) {
                flyAnim = null;
            }
            needsRender = true;
        }

        if (controls.update()) {
            needsRender = true;
        }

        if (needsRender) {
            needsRender = false;
            renderer.render(scene, camera);
        }
    });

    // Public API returned to exercise-library.js
    return {
        setView(view, group, key) {
            applyView(view, group, key);
        },
        select(key) {
            selectedKey = key || '';
            applySelection();
        },
        setLayer(layer) {
            currentLayerMode = layer;
            applyLayerVisibility();
            applySelection();
            requestRender();
        },
        dispose() {
            renderer.setAnimationLoop(null);
            resizeObserver.disconnect();
            scene.traverse((o) => {
                if (o.geometry) o.geometry.dispose();
                if (o.material) {
                    if (Array.isArray(o.material)) o.material.forEach(m => m.dispose());
                    else o.material.dispose();
                }
            });
            pmrem.dispose();
            renderer.dispose();
        }
    };
}
