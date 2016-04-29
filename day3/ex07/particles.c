// emcc -O2 -o index.html -s ASM_JS=1 -s NO_FILESYSTEM=1 --closure 1 particles.c
// emcc -O2 -o index.html -s NO_FILESYSTEM=1 -s INVOKE_RUN=1 -s "EXPORT_NAME='Foo'" -s MODULARIZE=1 --closure 1 particles.c

// f=new Float32Array(Module.HEAPF32.buffer, Module.HEAP32[5247920>>2], 384)
// Module.ccall("dump",null,['ptr'],[5247920])

#include <stdint.h>
#include <stdlib.h>
#include <emscripten.h>

typedef struct {
  float x,y,z;
} Vec3;

typedef struct {
  Vec3 pos;
  Vec3 vel;
  float age;
} Particle;

typedef struct {
  Particle *particles;
  uint32_t numParticles;
  Vec3 emitPos;
  Vec3 emitDir;
  Vec3 gravity;
} ParticleSystem;


Vec3* setVec3(Vec3 &v, float x, float y, float z) {
  v->x = x;
  v->y = y;
  v->z = z;
  return v;
}

Vec3* addVec3(Vec3 &v, Vec3 &v2) {
  v->x += v2->x;
  v->y += v2->y;
  v->z += v2->z;
  return v;
}

ParticleSystem* makeParticleSystem(uint32_t num) {
  ParticleSystem *psys = (ParticleSystem*)malloc(sizeof(ParticleSystem));
  psys->verts = (Particle*)malloc(num * sizeof(Particle));
  psys->numParticles = num;
  return psys;
}

ParticlesSystem* updateParticleSystem(ParticlesSystem *psys) {
  for(uint32_t i=0; i < psys->numParticles; i++) {
    Particle *p = psys->particles[i];
    addVec3(&(p->pos), &(p->vel));
    addVec3(&(p->pos), &(psys->gravity));
  }
}

EMSCRIPTEN_KEEPALIVE float dump(ParticleSystem *psys) {
  return (psys->particles[0]).y;
}

int main() {
  ParticleSystem *psys = makeParticleSystem(10);
  setVec3(&(psys->emitPos), 0.f, 0.f, 0.f);
  setVec3(&(psys->emitDir), 0.f, 1.f, 0.f);
  setVec3(&(psys->gravity), 0.f, -0.01f, 0.f);
  return (int)psys;
}
